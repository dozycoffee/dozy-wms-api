package com.dozycoffee.wms.outbound.application.service

import com.dozycoffee.wms.inventory.application.port.`in`.GetInventoryUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.GetLotUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.HoldInventoryUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.command.HoldInventoryCommand
import com.dozycoffee.wms.inventory.domain.enumeration.AllocationReferenceType
import com.dozycoffee.wms.inventory.domain.enumeration.QualityStatus
import com.dozycoffee.wms.outbound.domain.model.OutboundAllocation
import com.dozycoffee.wms.outbound.domain.model.OutboundItem
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Component

/** 출고 할당이 가리키는 Lot의 정상 재고를 WMS 재고에서 점유한다 */
@Component
class OutboundAllocationHolder(
    private val getLotUseCase: GetLotUseCase,
    private val getInventoryUseCase: GetInventoryUseCase,
    private val holdInventoryUseCase: HoldInventoryUseCase
) {

    /** 할당된 Lot의 정상 재고를 재고 행 ID 순으로 점유한다. 이 창고에 그 Lot의 가용 재고가 모자라면 확보한 만큼만 돌려준다 */
    suspend fun hold(item: OutboundItem, allocation: OutboundAllocation, warehouseId: Long): Int {
        val lot = getLotUseCase.getAllByProduct(item.productId).toList().find { it.lotNumber == allocation.lotNumber }
            ?: return 0
        val candidates = getInventoryUseCase.getAll(null, item.productId, QualityStatus.NORMAL, null, listOf(warehouseId)).toList()
            .filter { it.lotId == lot.lotId }
            .sortedBy { it.inventoryId }

        var remainingQuantity = allocation.quantity
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
        return allocation.quantity - remainingQuantity
    }
}
