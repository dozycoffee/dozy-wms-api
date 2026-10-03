package com.dozycoffee.wms.warehouse_member.application.port.`in`

import com.dozycoffee.wms.warehouse_member.application.port.`in`.result.WarehouseMemberResult
import kotlinx.coroutines.flow.Flow

interface GetWarehouseMemberUseCase {
    fun getAllByWarehouse(warehouseId: Long): Flow<WarehouseMemberResult>
}
