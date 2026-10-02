package com.dozycoffee.wms.warehouse_member.application.port.`in`.result

import com.dozycoffee.wms.warehouse_member.domain.model.WarehouseMember
import java.util.UUID

data class WarehouseMemberResult(
    val warehouseMemberId: Long,
    val warehouseId: Long,
    val principalId: UUID
) {
    companion object {
        fun from(warehouseMember: WarehouseMember): WarehouseMemberResult {
            return WarehouseMemberResult(
                requireNotNull(warehouseMember.warehouseMemberId),
                warehouseMember.warehouseId,
                warehouseMember.principalId
            )
        }
    }
}
