package com.dozycoffee.wms.outbound.application.service

import com.dozycoffee.wms.inventory.application.port.`in`.FulfillAllocationUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.GetAllocationUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.GetInventoryUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.GetLotUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.HoldInventoryUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.command.HoldInventoryCommand
import com.dozycoffee.wms.inventory.domain.enumeration.AllocationReferenceType
import com.dozycoffee.wms.inventory.domain.enumeration.QualityStatus
import com.dozycoffee.wms.outbound.application.port.`in`.CompleteOutboundUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.GetOutboundUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.RegisterOutboundUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.StartOutboundInspectingUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.StartOutboundPickingUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.command.RegisterOutboundCommand
import com.dozycoffee.wms.outbound.application.port.`in`.result.OutboundResult
import com.dozycoffee.wms.outbound.application.port.out.OutboundItemRepository
import com.dozycoffee.wms.outbound.application.port.out.OutboundRepository
import com.dozycoffee.wms.outbound.domain.enumeration.OutboundStatus
import com.dozycoffee.wms.outbound.domain.exception.OutboundNotFoundException
import com.dozycoffee.wms.outbound.domain.model.Outbound
import com.dozycoffee.wms.outbound.domain.model.OutboundItem
import com.dozycoffee.wms.warehouse.application.port.`in`.GetWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.OccupyWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.ReleaseLocationUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.ReleaseWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.command.OccupyWorkAreaCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.command.ReleaseLocationCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.command.ReleaseWorkAreaCommand
import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OutboundService(
    private val outboundRepository: OutboundRepository,
    private val outboundItemRepository: OutboundItemRepository,
    private val getInventoryUseCase: GetInventoryUseCase,
    private val getLotUseCase: GetLotUseCase,
    private val holdInventoryUseCase: HoldInventoryUseCase,
    private val getAllocationUseCase: GetAllocationUseCase,
    private val fulfillAllocationUseCase: FulfillAllocationUseCase,
    private val getWorkAreaUseCase: GetWorkAreaUseCase,
    private val occupyWorkAreaUseCase: OccupyWorkAreaUseCase,
    private val releaseWorkAreaUseCase: ReleaseWorkAreaUseCase,
    private val releaseLocationUseCase: ReleaseLocationUseCase
) : RegisterOutboundUseCase,
    StartOutboundPickingUseCase,
    StartOutboundInspectingUseCase,
    CompleteOutboundUseCase,
    GetOutboundUseCase {

    @Transactional
    override suspend fun register(command: RegisterOutboundCommand): OutboundResult {
        val outbound = Outbound.create(command.warehouseId)
        val savedOutbound = outboundRepository.save(outbound)

        command.items.forEach { item ->
            outboundItemRepository.save(
                OutboundItem.create(savedOutbound.outboundId, item.productId, item.requestedQuantity)
            )
        }

        return OutboundResult.from(savedOutbound)
    }

    /**
     * FIFO 피킹: 상품별로 `lot.expirationDate` 오름차순으로 정상(NORMAL) 재고를 점유(Allocation.HELD)한다.
     * 재고가 부족하면 확보 가능한 만큼만 피킹한다 — 부족분은 OutboundItem.shortageQuantity로 드러난다.
     */
    @Transactional
    override suspend fun startPicking(outboundId: Long): OutboundResult {
        val outbound = findOutboundOrThrow(outboundId)
        val items = outboundItemRepository.findAllByOutboundId(outboundId).toList()

        var totalPickedQuantity = 0
        for (item in items) {
            val pickedQuantity = pickFifo(item)
            item.pick(pickedQuantity)
            outboundItemRepository.save(item)
            totalPickedQuantity += pickedQuantity
        }

        val workArea = getWorkAreaUseCase.getByWarehouseIdAndAreaCode(outbound.warehouseId, AreaCode.OUTBOUND).awaitSingle()
        occupyWorkAreaUseCase.occupy(OccupyWorkAreaCommand(workArea.workAreaId, totalPickedQuantity)).awaitSingle()

        outbound.startPicking()
        return OutboundResult.from(outboundRepository.save(outbound))
    }

    @Transactional
    override suspend fun startInspecting(outboundId: Long): OutboundResult {
        val outbound = findOutboundOrThrow(outboundId)
        outbound.startInspecting()
        return OutboundResult.from(outboundRepository.save(outbound))
    }

    /** 검수를 마친 상품의 점유를 이행(fulfill) 확정하고, 물리적으로 반출된 만큼 Location/출고장 점유를 해제한다 */
    @Transactional
    override suspend fun complete(outboundId: Long): OutboundResult {
        val outbound = findOutboundOrThrow(outboundId)
        val items = outboundItemRepository.findAllByOutboundId(outboundId).toList()

        var totalPickedQuantity = 0
        for (item in items) {
            val allocations = getAllocationUseCase
                .getAllHeldByReference(AllocationReferenceType.OUTBOUND, requireNotNull(item.outboundItemId))
                .toList()
            for (allocation in allocations) {
                val inventory = getInventoryUseCase.getById(allocation.inventoryId)
                fulfillAllocationUseCase.fulfill(allocation.allocationId)
                releaseLocationUseCase.release(ReleaseLocationCommand(inventory.locationId, allocation.quantity)).awaitSingle()
            }
            totalPickedQuantity += item.pickedQuantity ?: 0
        }

        val workArea = getWorkAreaUseCase.getByWarehouseIdAndAreaCode(outbound.warehouseId, AreaCode.OUTBOUND).awaitSingle()
        releaseWorkAreaUseCase.release(ReleaseWorkAreaCommand(workArea.workAreaId, totalPickedQuantity)).awaitSingle()

        outbound.complete()
        return OutboundResult.from(outboundRepository.save(outbound))
    }

    @Transactional(readOnly = true)
    override suspend fun getById(outboundId: Long): OutboundResult {
        return OutboundResult.from(findOutboundOrThrow(outboundId))
    }

    @Transactional(readOnly = true)
    override fun getAll(status: OutboundStatus?): Flow<OutboundResult> {
        return outboundRepository.findAll(status).map { OutboundResult.from(it) }
    }

    private suspend fun pickFifo(item: OutboundItem): Int {
        val expirationDateByLotId = getLotUseCase.getAllByProduct(item.productId).toList()
            .associate { it.lotId to it.expirationDate }
        val candidates = getInventoryUseCase.getAll(null, item.productId, QualityStatus.NORMAL, null, null).toList()
            .sortedWith(compareBy(nullsLast()) { expirationDateByLotId[it.lotId] })

        var remainingQuantity = item.requestedQuantity
        for (inventory in candidates) {
            if (remainingQuantity <= 0) break
            val allocatableQuantity = minOf(inventory.availableQuantity, remainingQuantity)
            if (allocatableQuantity <= 0) continue

            holdInventoryUseCase.hold(
                HoldInventoryCommand(
                    inventory.inventoryId,
                    AllocationReferenceType.OUTBOUND,
                    requireNotNull(item.outboundItemId),
                    allocatableQuantity
                )
            )
            remainingQuantity -= allocatableQuantity
        }

        return item.requestedQuantity - remainingQuantity
    }

    private suspend fun findOutboundOrThrow(outboundId: Long): Outbound {
        return outboundRepository.findById(outboundId) ?: throw OutboundNotFoundException()
    }
}
