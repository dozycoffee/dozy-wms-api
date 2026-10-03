package com.dozycoffee.wms.warehouse_member.application.port.`in`

import java.util.UUID

interface RemoveWarehouseMemberUseCase {
    suspend fun remove(warehouseId: Long, principalId: UUID)
}
