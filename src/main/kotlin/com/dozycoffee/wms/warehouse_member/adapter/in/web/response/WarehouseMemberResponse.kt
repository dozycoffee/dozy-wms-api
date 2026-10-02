package com.dozycoffee.wms.warehouse_member.adapter.`in`.web.response

import com.dozycoffee.wms.warehouse_member.application.port.`in`.result.WarehouseMemberResult
import java.util.UUID

data class WarehouseMemberResponse(
    val warehouseMemberId: Long,
    val warehouseId: Long,
    val principalId: UUID
) {
    companion object {
        fun from(result: WarehouseMemberResult): WarehouseMemberResponse {
            return WarehouseMemberResponse(result.warehouseMemberId, result.warehouseId, result.principalId)
        }
    }
}
