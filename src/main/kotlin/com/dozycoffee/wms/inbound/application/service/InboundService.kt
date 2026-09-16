package com.dozycoffee.wms.inbound.application.service

import com.dozycoffee.wms.disposal.application.port.`in`.RegisterDisposalUseCase
import com.dozycoffee.wms.disposal.application.port.`in`.command.RegisterDisposalCommand
import com.dozycoffee.wms.disposal.application.port.`in`.command.RegisterDisposalItemCommand
import com.dozycoffee.wms.disposal.domain.enumeration.DisposalReason
import com.dozycoffee.wms.inbound.application.port.`in`.CompleteInboundUseCase
import com.dozycoffee.wms.inbound.application.port.`in`.GetInboundUseCase
import com.dozycoffee.wms.inbound.application.port.`in`.RegisterInboundUseCase
import com.dozycoffee.wms.inbound.application.port.`in`.StartInboundProcessingUseCase
import com.dozycoffee.wms.inbound.application.port.`in`.command.CompleteInboundCommand
import com.dozycoffee.wms.inbound.application.port.`in`.command.LotAssignmentCommand
import com.dozycoffee.wms.inbound.application.port.`in`.command.RegisterInboundCommand
import com.dozycoffee.wms.inbound.application.port.`in`.result.InboundResult
import com.dozycoffee.wms.inbound.application.port.out.InboundItemRepository
import com.dozycoffee.wms.inbound.application.port.out.InboundRepository
import com.dozycoffee.wms.inbound.domain.enumeration.InboundStatus
import com.dozycoffee.wms.inbound.domain.enumeration.InspectionResult
import com.dozycoffee.wms.inbound.domain.exception.InboundNotFoundException
import com.dozycoffee.wms.inbound.domain.exception.InsufficientZoneCapacityException
import com.dozycoffee.wms.inbound.domain.exception.MissingLotAssignmentException
import com.dozycoffee.wms.inbound.domain.exception.NotAllItemsInspectedException
import com.dozycoffee.wms.inbound.domain.model.Inbound
import com.dozycoffee.wms.inbound.domain.model.InboundItem
import com.dozycoffee.wms.inventory.application.port.`in`.GetLotUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.MarkInventoryDefectiveUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.RegisterInventoryUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.RegisterLotUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.command.RegisterInventoryCommand
import com.dozycoffee.wms.inventory.application.port.`in`.command.RegisterLotCommand
import com.dozycoffee.wms.inventory.application.port.`in`.result.InventoryResult
import com.dozycoffee.wms.inventory.application.port.`in`.result.LotResult
import com.dozycoffee.wms.product.application.port.`in`.GetProductUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.GetLocationUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.GetWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.GetZoneUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.OccupyLocationUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.OccupyWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.ReleaseWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.command.OccupyLocationCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.command.OccupyWorkAreaCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.command.ReleaseWorkAreaCommand
import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class InboundService(
    private val inboundRepository: InboundRepository,
    private val inboundItemRepository: InboundItemRepository,
    private val getProductUseCase: GetProductUseCase,
    private val getZoneUseCase: GetZoneUseCase,
    private val getLocationUseCase: GetLocationUseCase,
    private val occupyLocationUseCase: OccupyLocationUseCase,
    private val getWorkAreaUseCase: GetWorkAreaUseCase,
    private val occupyWorkAreaUseCase: OccupyWorkAreaUseCase,
    private val releaseWorkAreaUseCase: ReleaseWorkAreaUseCase,
    private val getLotUseCase: GetLotUseCase,
    private val registerLotUseCase: RegisterLotUseCase,
    private val registerInventoryUseCase: RegisterInventoryUseCase,
    private val markInventoryDefectiveUseCase: MarkInventoryDefectiveUseCase,
    private val registerDisposalUseCase: RegisterDisposalUseCase
) : RegisterInboundUseCase, StartInboundProcessingUseCase, CompleteInboundUseCase, GetInboundUseCase {

    /**
     * 등록 시 목적지 Zone(들)의 잔여 capacity를 사전 점검해 부족하면 반려한다 (ADR-0003).
     * 통과하면 바로 WAITING까지 전환한다 — 사전 점검을 통과한 이상 EXPECTED로 남아있을 이유가 없다.
     */
    @Transactional
    override suspend fun register(command: RegisterInboundCommand): InboundResult {
        val zoneCodeByItem = command.items.associateWith {
            val product = getProductUseCase.getById(it.productId)
            ZoneCode.valueOf(product.category.zoneCode)
        }
        val requiredQuantityByZoneCode = command.items
            .groupBy { zoneCodeByItem.getValue(it) }
            .mapValues { (_, items) -> items.sumOf { item -> item.expectedQuantity } }

        val zoneIdByCode = mutableMapOf<ZoneCode, Long>()
        for ((zoneCode, requiredQuantity) in requiredQuantityByZoneCode) {
            val zone = getZoneUseCase.getByWarehouseIdAndZoneCode(command.warehouseId, zoneCode).awaitSingle()
            zoneIdByCode[zoneCode] = zone.zoneId
            val locations = getLocationUseCase.getByZoneId(zone.zoneId).collectList().awaitSingle()
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
                InboundItem.create(savedInbound.inboundId, item.productId, zoneId, item.expectedQuantity)
            )
        }

        return InboundResult.from(savedInbound)
    }

    @Transactional
    override suspend fun startProcessing(inboundId: Long): InboundResult {
        val inbound = findInboundOrThrow(inboundId)
        val totalExpectedQuantity = inboundItemRepository.findAllByInboundId(inboundId).toList()
            .sumOf { it.expectedQuantity }

        val workArea = getWorkAreaUseCase.getByWarehouseIdAndAreaCode(inbound.warehouseId, AreaCode.INBOUND).awaitSingle()
        occupyWorkAreaUseCase.occupy(OccupyWorkAreaCommand(workArea.workAreaId, totalExpectedQuantity)).awaitSingle()

        inbound.startProcessing()
        return InboundResult.from(inboundRepository.save(inbound))
    }

    /**
     * 정상/불량 판정 상품 모두 Lot을 확정하고 Zone 내 Location에 분산 배치해 Inventory로 등록한다.
     * 불량 판정 상품은 등록 직후 DEFECTIVE로 전환하고 Disposal(REQUESTED, INSPECTION_DEFECT)로 연계한다 —
     * 폐기 처리장 점유 등 물리적 처리는 이후 Disposal 승인/완료 단계에서 이뤄진다.
     */
    @Transactional
    override suspend fun complete(command: CompleteInboundCommand): InboundResult {
        val inbound = findInboundOrThrow(command.inboundId)
        val items = inboundItemRepository.findAllByInboundId(command.inboundId).toList()

        if (items.any { it.inspectionResult == InspectionResult.PENDING }) {
            throw NotAllItemsInspectedException()
        }

        val lotAssignmentByItemId = command.lotAssignments.associateBy { it.inboundItemId }
        val defectiveDisposalItems = mutableListOf<RegisterDisposalItemCommand>()
        items.forEach { item ->
            val assignment = lotAssignmentByItemId[item.inboundItemId] ?: throw MissingLotAssignmentException()
            val lot = resolveLot(item.productId, assignment)
            val registeredInventories = distributeToLocations(item, lot.lotId)

            if (item.inspectionResult == InspectionResult.DEFECTIVE) {
                registeredInventories.forEach { inventory ->
                    markInventoryDefectiveUseCase.markDefective(inventory.inventoryId)
                    defectiveDisposalItems.add(
                        RegisterDisposalItemCommand(inventory.inventoryId, inventory.quantity, DisposalReason.INSPECTION_DEFECT)
                    )
                }
            }
        }

        if (defectiveDisposalItems.isNotEmpty()) {
            registerDisposalUseCase.register(RegisterDisposalCommand(inbound.warehouseId, defectiveDisposalItems))
        }

        val totalExpectedQuantity = items.sumOf { it.expectedQuantity }
        val workArea = getWorkAreaUseCase.getByWarehouseIdAndAreaCode(inbound.warehouseId, AreaCode.INBOUND).awaitSingle()
        releaseWorkAreaUseCase.release(ReleaseWorkAreaCommand(workArea.workAreaId, totalExpectedQuantity)).awaitSingle()

        inbound.complete()
        return InboundResult.from(inboundRepository.save(inbound))
    }

    @Transactional(readOnly = true)
    override suspend fun getById(inboundId: Long): InboundResult {
        return InboundResult.from(findInboundOrThrow(inboundId))
    }

    @Transactional(readOnly = true)
    override fun getAll(status: InboundStatus?): Flow<InboundResult> {
        return inboundRepository.findAll(status).map { InboundResult.from(it) }
    }

    private suspend fun resolveLot(productId: Long, assignment: LotAssignmentCommand): LotResult {
        val existingLot = getLotUseCase.getAllByProduct(productId).toList()
            .find { it.lotNumber == assignment.lotNumber }
        if (existingLot != null) {
            return existingLot
        }
        return registerLotUseCase.register(
            RegisterLotCommand(assignment.lotNumber, productId, assignment.manufactureDate, assignment.expirationDate)
        )
    }

    /** Zone 내 잔여 capacity가 큰 Location부터 채우는 First-Fit 방식으로 여러 Location에 분산 배치한다 */
    private suspend fun distributeToLocations(item: InboundItem, lotId: Long): List<InventoryResult> {
        var remainingQuantity = requireNotNull(item.actualQuantity)
        val locations = getLocationUseCase.getByZoneId(item.zoneId).collectList().awaitSingle()
            .sortedByDescending { it.maxCapacity - it.usedCapacity }

        val registeredInventories = mutableListOf<InventoryResult>()
        for (location in locations) {
            if (remainingQuantity <= 0) break
            val availableCapacity = location.maxCapacity - location.usedCapacity
            if (availableCapacity <= 0) continue

            val allocatedQuantity = minOf(availableCapacity, remainingQuantity)
            occupyLocationUseCase.occupy(OccupyLocationCommand(location.locationId, allocatedQuantity)).awaitSingle()
            registeredInventories.add(
                registerInventoryUseCase.register(
                    RegisterInventoryCommand(lotId, location.locationId, allocatedQuantity, requireNotNull(item.inboundItemId))
                )
            )
            remainingQuantity -= allocatedQuantity
        }

        if (remainingQuantity > 0) {
            throw InsufficientZoneCapacityException()
        }
        return registeredInventories
    }

    private suspend fun findInboundOrThrow(inboundId: Long): Inbound {
        return inboundRepository.findById(inboundId) ?: throw InboundNotFoundException()
    }
}
