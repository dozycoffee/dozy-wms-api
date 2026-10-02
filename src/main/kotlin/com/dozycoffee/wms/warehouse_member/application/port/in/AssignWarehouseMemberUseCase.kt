package com.dozycoffee.wms.warehouse_member.application.port.`in`

import com.dozycoffee.wms.warehouse_member.application.port.`in`.result.WarehouseMemberResult
import java.util.UUID

interface AssignWarehouseMemberUseCase {
    /** 이미 배정돼 있으면 기존 배정을 그대로 반환한다 */
    suspend fun assign(warehouseId: Long, principalId: UUID): WarehouseMemberResult
}
