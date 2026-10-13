package com.dozycoffee.wms.inventory_call.application.port.`in`

import com.dozycoffee.wms.inventory_call.application.port.`in`.result.InventoryCallResult

interface RequeueInventoryCallUseCase {
    /** 격리(DEAD)된 호출을 다시 전송 대기로 되돌린다 */
    suspend fun requeue(inventoryCallId: Long): InventoryCallResult
}
