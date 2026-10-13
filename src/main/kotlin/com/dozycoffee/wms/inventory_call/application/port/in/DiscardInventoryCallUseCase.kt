package com.dozycoffee.wms.inventory_call.application.port.`in`

import com.dozycoffee.wms.inventory_call.application.port.`in`.result.InventoryCallResult

interface DiscardInventoryCallUseCase {
    /** 격리(DEAD)된 호출을 전송하지 않기로 하고 그 창고의 큐를 푼다 */
    suspend fun discard(inventoryCallId: Long): InventoryCallResult
}
