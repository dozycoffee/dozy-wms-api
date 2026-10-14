package com.dozycoffee.wms.outbound.application.service

import com.dozycoffee.wms.location_stock.application.port.`in`.HoldLocationStockUseCase
import com.dozycoffee.wms.outbound.application.port.out.OutboundAllocationPickRepository
import com.dozycoffee.wms.outbound.domain.model.OutboundAllocation
import com.dozycoffee.wms.outbound.domain.model.OutboundAllocationPick
import com.dozycoffee.wms.outbound.domain.model.OutboundItem
import org.springframework.stereotype.Component

/**
 * 출고 할당의 피킹 계획과 위치 재고 점유를 관리한다 (ADR-0024). 계획은 [OutboundAllocationPick]에 남고 계획이 잡힌 수량은
 * 장부 행의 점유 수량이다. 수량·예약의 진실은 inventory이고, 이 점유는 WMS 안에서 같은 재고를 두 번 꺼내지 않게 한다.
 */
@Component
class OutboundAllocationHolder(
    private val holdLocationStockUseCase: HoldLocationStockUseCase,
    private val outboundAllocationPickRepository: OutboundAllocationPickRepository
) {

    /** 할당된 Lot의 정상 재고를 가용 수량이 많은 Location부터 점유하고 피킹 계획을 남긴다. 모자라면 확보한 만큼만 돌려준다 */
    suspend fun hold(item: OutboundItem, allocation: OutboundAllocation, warehouseId: Long): Int {
        val allocationId: Long = requireNotNull(allocation.outboundAllocationId)
        val held = holdLocationStockUseCase.holdForOutbound(warehouseId, item.productId, allocation.lotNumber, allocation.quantity)
        held.forEach {
            outboundAllocationPickRepository.save(OutboundAllocationPick.create(allocationId, it.locationStockId, it.quantity))
        }
        return held.sumOf { it.quantity }
    }

    /** 피킹 계획을 지우고 점유를 푼다. 취소·재할당 때 쓴다 */
    suspend fun release(allocation: OutboundAllocation) {
        val allocationId: Long = requireNotNull(allocation.outboundAllocationId)
        outboundAllocationPickRepository.findAllByOutboundAllocationIds(listOf(allocationId)).forEach {
            holdLocationStockUseCase.releaseHold(it.locationStockId, it.quantity)
        }
        outboundAllocationPickRepository.deleteAllByOutboundAllocationId(allocationId)
    }

    /** 피킹 계획대로 실제로 꺼낸다. 위치 재고와 Location 사용량이 줄고 계획은 기록으로 남는다 */
    suspend fun consume(allocation: OutboundAllocation) {
        val allocationId: Long = requireNotNull(allocation.outboundAllocationId)
        outboundAllocationPickRepository.findAllByOutboundAllocationIds(listOf(allocationId)).forEach {
            holdLocationStockUseCase.consume(it.locationStockId, it.quantity)
        }
    }
}
