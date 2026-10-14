package com.dozycoffee.wms.inbound.application.service

import com.dozycoffee.wms.disposal.application.port.`in`.RegisterDisposalUseCase
import com.dozycoffee.wms.disposal.application.port.`in`.command.RegisterDisposalCommand
import com.dozycoffee.wms.disposal.application.port.`in`.command.RegisterDisposalItemCommand
import com.dozycoffee.wms.disposal.domain.enumeration.DisposalReason
import com.dozycoffee.wms.global.security.WarehouseAccessGuard
import com.dozycoffee.wms.inbound.application.port.`in`.CompleteInboundUseCase
import com.dozycoffee.wms.inbound.application.port.`in`.GetInboundUseCase
import com.dozycoffee.wms.inbound.application.port.`in`.RegisterInboundUseCase
import com.dozycoffee.wms.inbound.application.port.`in`.StartInboundProcessingUseCase
import com.dozycoffee.wms.inbound.application.port.`in`.command.CompleteInboundCommand
import com.dozycoffee.wms.inbound.application.port.`in`.command.RegisterInboundCommand
import com.dozycoffee.wms.inbound.application.port.`in`.result.InboundResult
import com.dozycoffee.wms.inbound.application.port.out.InboundItemRepository
import com.dozycoffee.wms.inbound.application.port.out.InboundReceiptRepository
import com.dozycoffee.wms.inbound.application.port.out.InboundRepository
import com.dozycoffee.wms.inbound.domain.enumeration.InboundStatus
import com.dozycoffee.wms.inbound.domain.enumeration.DefectReason
import com.dozycoffee.wms.inbound.domain.enumeration.InspectionResult
import com.dozycoffee.wms.inbound.domain.enumeration.InspectionStatus
import com.dozycoffee.wms.inbound.domain.exception.InboundNotFoundException
import com.dozycoffee.wms.inbound.domain.exception.InsufficientZoneCapacityException
import com.dozycoffee.wms.inbound.domain.exception.NotAllItemsInspectedException
import com.dozycoffee.wms.inbound.domain.model.Inbound
import com.dozycoffee.wms.inbound.domain.model.InboundItem
import com.dozycoffee.wms.inbound.domain.model.InboundReceipt
import com.dozycoffee.wms.location_stock.application.port.`in`.IncreaseLocationStockUseCase
import com.dozycoffee.wms.location_stock.application.port.`in`.command.IncreaseLocationStockCommand
import com.dozycoffee.wms.location_stock.domain.enumeration.StockQualityStatus
import com.dozycoffee.wms.product.application.port.`in`.GetProductUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.GetLocationUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.GetWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.GetZoneUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.OccupyWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.ReleaseWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.command.OccupyWorkAreaCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.command.ReleaseWorkAreaCommand
import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class InboundService(
    private val warehouseAccessGuard: WarehouseAccessGuard,
    private val inboundRepository: InboundRepository,
    private val inboundItemRepository: InboundItemRepository,
    private val inboundReceiptRepository: InboundReceiptRepository,
    private val getProductUseCase: GetProductUseCase,
    private val getZoneUseCase: GetZoneUseCase,
    private val getLocationUseCase: GetLocationUseCase,
    private val getWorkAreaUseCase: GetWorkAreaUseCase,
    private val occupyWorkAreaUseCase: OccupyWorkAreaUseCase,
    private val releaseWorkAreaUseCase: ReleaseWorkAreaUseCase,
    private val increaseLocationStockUseCase: IncreaseLocationStockUseCase,
    private val registerDisposalUseCase: RegisterDisposalUseCase,
    private val inboundInventoryCallRecorder: InboundInventoryCallRecorder
) : RegisterInboundUseCase, StartInboundProcessingUseCase, CompleteInboundUseCase, GetInboundUseCase {

    /**
     * 등록 시 목적지 Zone(들)의 잔여 capacity를 사전 점검해 부족하면 반려한다 (ADR-0003).
     * 통과하면 바로 WAITING까지 전환한다 — 사전 점검을 통과한 이상 EXPECTED로 남아있을 이유가 없다.
     */
    @Transactional
    override suspend fun register(command: RegisterInboundCommand): InboundResult {
        warehouseAccessGuard.require(command.warehouseId)
        val zoneCodeByItem = command.items.associateWith {
            val product = getProductUseCase.getById(it.productId)
            product.category.zoneCode
        }
        val requiredQuantityByZoneCode = command.items
            .groupBy { zoneCodeByItem.getValue(it) }
            .mapValues { (_, items) -> items.sumOf { item -> item.expectedQuantity } }

        val zoneIdByCode = mutableMapOf<ZoneCode, Long>()
        for ((zoneCode, requiredQuantity) in requiredQuantityByZoneCode) {
            val zone = getZoneUseCase.getByWarehouseIdAndZoneCode(command.warehouseId, zoneCode)
            zoneIdByCode[zoneCode] = zone.zoneId
            val locations = getLocationUseCase.getByZoneId(zone.zoneId).toList()
            val remainingCapacity = locations.sumOf { it.maxCapacity - it.usedCapacity }
            if (remainingCapacity < requiredQuantity) {
                throw InsufficientZoneCapacityException()
            }
        }

        val inbound = Inbound.create(command.warehouseId, command.expectedArrivalDate)
        inbound.markWaiting()
        val savedInbound = inboundRepository.save(inbound)

        command.items.forEach { item ->
            val zoneId = requireNotNull(zoneIdByCode[zoneCodeByItem.getValue(item)])
            inboundItemRepository.save(
                InboundItem.create(
                    savedInbound.inboundId,
                    item.productId,
                    zoneId,
                    item.expectedQuantity,
                    item.expectedLotNumber,
                    item.expectedExpirationDate
                )
            )
        }

        return InboundResult.from(savedInbound)
    }

    @Transactional
    override suspend fun startProcessing(inboundId: Long): InboundResult {
        val inbound = findAccessibleInboundOrThrow(inboundId)
        val totalExpectedQuantity = inboundItemRepository.findAllByInboundId(inboundId).toList()
            .sumOf { it.expectedQuantity }

        val workArea = getWorkAreaUseCase.getByWarehouseIdAndAreaCode(inbound.warehouseId, AreaCode.INBOUND)
        occupyWorkAreaUseCase.occupy(OccupyWorkAreaCommand(workArea.workAreaId, totalExpectedQuantity))

        inbound.startProcessing()
        return InboundResult.from(inboundRepository.save(inbound))
    }

    /**
     * 수령 라인마다 Zone 내 Location에 분산 배치해 Location 장부에 반영한다(Location 사용량은 장부가 갱신한다, ADR-0024).
     * 불량 라인은 불량(DEFECTIVE) 품질로 장부에 반영하고 Disposal(REQUESTED)로 연계한다 — 폐기 처리장 점유 등 물리적 처리는
     * 이후 Disposal 승인/완료 단계에서 이뤄진다. 수령 라인마다 inventory 입고 확정 호출을 같은 트랜잭션에서 아웃박스에 기록한다.
     */
    @Transactional
    override suspend fun complete(command: CompleteInboundCommand): InboundResult {
        val inbound = findAccessibleInboundOrThrow(command.inboundId)
        val items = inboundItemRepository.findAllByInboundId(command.inboundId).toList()

        if (items.any { it.inspectionStatus == InspectionStatus.PENDING }) {
            throw NotAllItemsInspectedException()
        }

        val receiptsByItemId: Map<Long, List<InboundReceipt>> = inboundReceiptRepository
            .findAllByInboundItemIds(items.mapNotNull { it.inboundItemId })
            .groupBy { it.inboundItemId }
        val defectiveDisposalItems = mutableListOf<RegisterDisposalItemCommand>()
        items.forEach { item ->
            receiptsByItemId[item.inboundItemId].orEmpty().forEach { receipt ->
                val defective = receipt.inspectionResult == InspectionResult.DEFECTIVE
                val placements = distributeToLocations(item, receipt, if (defective) StockQualityStatus.DEFECTIVE else StockQualityStatus.NORMAL)
                inboundInventoryCallRecorder.record(inbound.warehouseId, item.productId, receipt)

                if (defective) {
                    placements.forEach { placement ->
                        defectiveDisposalItems.add(
                            RegisterDisposalItemCommand(
                                placement.locationStockId,
                                placement.quantity,
                                toDisposalReason(requireNotNull(receipt.defectReason))
                            )
                        )
                    }
                }
            }
        }

        if (defectiveDisposalItems.isNotEmpty()) {
            registerDisposalUseCase.register(RegisterDisposalCommand(inbound.warehouseId, defectiveDisposalItems))
        }

        val totalExpectedQuantity = items.sumOf { it.expectedQuantity }
        val workArea = getWorkAreaUseCase.getByWarehouseIdAndAreaCode(inbound.warehouseId, AreaCode.INBOUND)
        releaseWorkAreaUseCase.release(ReleaseWorkAreaCommand(workArea.workAreaId, totalExpectedQuantity))

        inbound.complete()
        return InboundResult.from(inboundRepository.save(inbound))
    }

    @Transactional(readOnly = true)
    override suspend fun getById(inboundId: Long): InboundResult {
        return InboundResult.from(findAccessibleInboundOrThrow(inboundId))
    }

    @Transactional(readOnly = true)
    override fun getAll(status: InboundStatus?): Flow<InboundResult> {
        return warehouseAccessGuard.scoped { warehouseIds -> inboundRepository.findAll(status, warehouseIds) }
            .map { InboundResult.from(it) }
    }

    /** Zone 내 잔여 capacity가 큰 Location부터 채우는 First-Fit 방식으로 여러 Location에 분산 배치한다 */
    private suspend fun distributeToLocations(
        item: InboundItem,
        receipt: InboundReceipt,
        qualityStatus: StockQualityStatus
    ): List<Placement> {
        var remainingQuantity = receipt.quantity
        val locations = getLocationUseCase.getByZoneId(item.zoneId).toList()
            .sortedByDescending { it.maxCapacity - it.usedCapacity }

        val placements = mutableListOf<Placement>()
        for (location in locations) {
            if (remainingQuantity <= 0) break
            val availableCapacity = location.maxCapacity - location.usedCapacity
            if (availableCapacity <= 0) continue

            val allocatedQuantity = minOf(availableCapacity, remainingQuantity)
            val stock = increaseLocationStockUseCase.increase(
                IncreaseLocationStockCommand(location.locationId, item.productId, receipt.lotNumber, qualityStatus, allocatedQuantity)
            )
            placements.add(Placement(stock.locationStockId, allocatedQuantity))
            remainingQuantity -= allocatedQuantity
        }

        if (remainingQuantity > 0) {
            throw InsufficientZoneCapacityException()
        }
        return placements
    }

    private data class Placement(val locationStockId: Long, val quantity: Int)

    private fun toDisposalReason(defectReason: DefectReason): DisposalReason {
        return if (defectReason == DefectReason.EXPIRED) DisposalReason.EXPIRED else DisposalReason.INSPECTION_DEFECT
    }

    private suspend fun findAccessibleInboundOrThrow(inboundId: Long): Inbound {
        val inbound = inboundRepository.findById(inboundId) ?: throw InboundNotFoundException()
        warehouseAccessGuard.require(inbound.warehouseId)
        return inbound
    }
}
