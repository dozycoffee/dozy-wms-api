package com.dozycoffee.wms.outbound.application.service

import com.dozycoffee.wms.global.security.WarehouseAccessGuard
import com.dozycoffee.wms.inventory.application.port.`in`.GetAllocationUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.ReleaseAllocationUseCase
import com.dozycoffee.wms.inventory.domain.enumeration.AllocationReferenceType
import com.dozycoffee.wms.outbound.application.port.`in`.ReportPickingShortageUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.command.ReportPickingShortageCommand
import com.dozycoffee.wms.outbound.application.port.`in`.result.OutboundAllocationResult
import com.dozycoffee.wms.outbound.application.port.out.OutboundAllocationRepository
import com.dozycoffee.wms.outbound.application.port.out.OutboundItemRepository
import com.dozycoffee.wms.outbound.application.port.out.OutboundRepository
import com.dozycoffee.wms.outbound.domain.exception.InvalidReallocationException
import com.dozycoffee.wms.outbound.domain.exception.OutboundNotFoundException
import com.dozycoffee.wms.outbound.domain.model.Outbound
import com.dozycoffee.wms.outbound.domain.model.OutboundAllocation
import com.dozycoffee.wms.outbound.domain.model.OutboundItem
import com.dozycoffee.wms.warehouse.application.port.`in`.GetWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.OccupyWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.ReleaseWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.command.OccupyWorkAreaCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.command.ReleaseWorkAreaCommand
import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 피킹 불가 Lot을 inventory에 보고해 재할당받고, 응답대로 출고 할당과 WMS 재고 점유를 다시 잡는다.
 * 로컬 변경은 inventory가 받아들인 뒤에만 하며(거부·불가면 예외로 아무것도 바뀌지 않는다), inventory 멱등 키의 회차는 로컬 반영 뒤에만 올린다.
 * 같은 회차로 다시 요청하면 inventory가 처음과 같은 응답을 주므로 로컬 반영이 롤백돼도 재시도할 수 있다.
 */
@Service
class OutboundReallocationService(
    private val warehouseAccessGuard: WarehouseAccessGuard,
    private val outboundRepository: OutboundRepository,
    private val outboundItemRepository: OutboundItemRepository,
    private val outboundAllocationRepository: OutboundAllocationRepository,
    private val outboundInventoryReallocator: OutboundInventoryReallocator,
    private val outboundAllocationHolder: OutboundAllocationHolder,
    private val getAllocationUseCase: GetAllocationUseCase,
    private val releaseAllocationUseCase: ReleaseAllocationUseCase,
    private val getWorkAreaUseCase: GetWorkAreaUseCase,
    private val occupyWorkAreaUseCase: OccupyWorkAreaUseCase,
    private val releaseWorkAreaUseCase: ReleaseWorkAreaUseCase
) : ReportPickingShortageUseCase {

    @Transactional
    override suspend fun report(command: ReportPickingShortageCommand): List<OutboundAllocationResult> {
        val outbound = outboundRepository.findById(command.outboundId) ?: throw OutboundNotFoundException()
        warehouseAccessGuard.require(outbound.warehouseId)
        outbound.requireReallocatable()
        val reservationId: Long = requireNotNull(outbound.reservationId)

        val items = outboundItemRepository.findAllByOutboundId(command.outboundId).toList()
        val allocationsByItemId: Map<Long, List<OutboundAllocation>> = outboundAllocationRepository
            .findAllByOutboundItemIds(items.mapNotNull { it.outboundItemId })
            .groupBy { it.outboundItemId }
        val shortageTargets = resolveShortages(command, allocationsByItemId.values.flatten())

        val reallocated = outboundInventoryReallocator.reallocate(
            reservationId,
            "wms-reallocation-${command.outboundId}-${outbound.reallocationCount + 1}",
            shortageTargets.map { OutboundInventoryReallocator.Shortage(it.first.inventoryId, it.second) }
        )

        var pickedQuantityDelta = 0
        val results = mutableListOf<OutboundAllocationResult>()
        for (reallocatedItem in reallocated) {
            val item = items.find { it.productId == reallocatedItem.productId } ?: continue
            pickedQuantityDelta += applyToItem(outbound, item, allocationsByItemId[item.outboundItemId].orEmpty(), reallocatedItem, results)
        }
        adjustWorkArea(outbound, pickedQuantityDelta)

        outbound.markReallocated()
        outboundRepository.save(outbound)
        return results
    }

    private fun resolveShortages(
        command: ReportPickingShortageCommand,
        allocations: List<OutboundAllocation>
    ): List<Pair<OutboundAllocation, Int>> {
        val byId = allocations.associateBy { it.outboundAllocationId }
        if (command.shortages.isEmpty() || command.shortages.map { it.outboundAllocationId }.distinct().size != command.shortages.size) {
            throw InvalidReallocationException()
        }
        return command.shortages.map { shortage ->
            val allocation = byId[shortage.outboundAllocationId] ?: throw InvalidReallocationException()
            if (shortage.shortageQuantity < 1 || shortage.shortageQuantity > allocation.quantity) throw InvalidReallocationException()
            allocation to shortage.shortageQuantity
        }
    }

    /** 한 출고 상품의 할당을 재할당 응답에 맞추고 레거시 점유를 새 할당 기준으로 다시 잡는다. 피킹 수량의 증감을 돌려준다 */
    private suspend fun applyToItem(
        outbound: Outbound,
        item: OutboundItem,
        existing: List<OutboundAllocation>,
        reallocated: OutboundInventoryReallocator.ReallocatedItem,
        results: MutableList<OutboundAllocationResult>
    ): Int {
        val outboundItemId: Long = requireNotNull(item.outboundItemId)
        getAllocationUseCase.getAllHeldByReference(AllocationReferenceType.OUTBOUND, outboundItemId).toList()
            .forEach { releaseAllocationUseCase.release(it.allocationId) }

        val newByInventoryId = reallocated.allocations.associateBy { it.inventoryId }
        val current = mutableListOf<OutboundAllocation>()
        for (allocation in existing) {
            val updated = newByInventoryId[allocation.inventoryId]
            if (updated == null) {
                outboundAllocationRepository.delete(allocation)
            } else {
                allocation.reallocate(updated.quantity)
                current.add(allocation)
            }
        }
        val existingInventoryIds = existing.map { it.inventoryId }.toSet()
        reallocated.allocations.filter { it.inventoryId !in existingInventoryIds }.forEach {
            current.add(OutboundAllocation.create(outboundItemId, it.inventoryId, it.lotNumber, it.expirationDate, it.quantity))
        }

        var pickedQuantity = 0
        for (allocation in current) {
            val held = outboundAllocationHolder.hold(item, allocation, outbound.warehouseId)
            allocation.pick(held)
            results.add(OutboundAllocationResult.from(outboundAllocationRepository.save(allocation), item.productId))
            pickedQuantity += held
        }
        val previousPickedQuantity: Int = item.pickedQuantity ?: 0
        item.repick(pickedQuantity)
        outboundItemRepository.save(item)
        return pickedQuantity - previousPickedQuantity
    }

    private suspend fun adjustWorkArea(outbound: Outbound, pickedQuantityDelta: Int) {
        if (pickedQuantityDelta == 0) return
        val workArea = getWorkAreaUseCase.getByWarehouseIdAndAreaCode(outbound.warehouseId, AreaCode.OUTBOUND)
        if (pickedQuantityDelta > 0) {
            occupyWorkAreaUseCase.occupy(OccupyWorkAreaCommand(workArea.workAreaId, pickedQuantityDelta))
        } else {
            releaseWorkAreaUseCase.release(ReleaseWorkAreaCommand(workArea.workAreaId, -pickedQuantityDelta))
        }
    }
}
