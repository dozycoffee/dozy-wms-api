package com.dozycoffee.wms.location_stock.application.port.`in`

import com.dozycoffee.wms.location_stock.application.port.`in`.result.HeldLocationStockResult

/** 출고 피킹 계획의 점유(ADR-0024). 수량·예약의 진실은 inventory이고 이 점유는 WMS 안에서 같은 재고를 두 번 꺼내지 않게 한다 */
interface HoldLocationStockUseCase {
    /**
     * 창고에서 상품·Lot 번호의 정상 재고를 가용 수량이 많은 Location부터 [quantity]만큼 점유한다.
     * 가용 수량이 모자라면 확보한 만큼만 점유하고 그 목록을 돌려준다.
     */
    suspend fun holdForOutbound(warehouseId: Long, productId: Long, lotNumber: String, quantity: Int): List<HeldLocationStockResult>

    suspend fun releaseHold(locationStockId: Long, quantity: Int)

    /** 점유한 수량을 실제로 내보낸다. 총 수량과 점유 수량이 함께 줄고 Location 사용량이 풀린다 */
    suspend fun consume(locationStockId: Long, quantity: Int)
}
