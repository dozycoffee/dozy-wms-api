package com.dozycoffee.wms.inventory_call.application.port.`in`

interface CheckInventoryCallBacklogUseCase {
    /** 창고에 아직 보내지 못한(PENDING) 호출이나 격리된(DEAD) 호출이 있는지 */
    suspend fun hasUnfinished(warehouseId: Long): Boolean
}
