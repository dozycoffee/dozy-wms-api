package com.dozycoffee.wms.location_stock.application.port.`in`

import com.dozycoffee.wms.location_stock.application.port.`in`.result.HeldLocationStockResult

interface ScheduleLocationStockDisposalUseCase {
    /**
     * 유통기한이 지난 Lot의 정상 재고 [quantity]를 가용 수량이 많은 Location부터(같으면 Location 코드 오름차순) 폐기 예정으로 옮긴다.
     * 점유된 수량은 옮기지 않으며 정상 재고가 모자라면 옮길 수 있는 만큼만 옮긴다. 폐기 예정 행과 옮긴 수량을 돌려준다.
     */
    suspend fun moveToDisposalScheduled(warehouseId: Long, productId: Long, lotNumber: String, quantity: Int): List<HeldLocationStockResult>
}
