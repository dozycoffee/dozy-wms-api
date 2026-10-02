package com.dozycoffee.wms.warehouse_member.domain.model

import com.dozycoffee.wms.global.error.DomainValidator.requireNonNull
import com.dozycoffee.wms.warehouse_member.domain.exception.WarehouseMemberErrorCode
import java.util.UUID

/** 사용자(Auth `principalId`)가 접근할 수 있는 창고 배정. Auth는 창고를 모르므로 WMS가 직접 관리한다(ADR-0012) */
class WarehouseMember private constructor(
    val warehouseMemberId: Long?,
    val warehouseId: Long,
    val principalId: UUID
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WarehouseMember) return false
        return warehouseMemberId != null && warehouseMemberId == other.warehouseMemberId
    }

    override fun hashCode(): Int = warehouseMemberId?.hashCode() ?: 0

    companion object {

        fun create(warehouseId: Long?, principalId: UUID?): WarehouseMember {
            return WarehouseMember(
                warehouseMemberId = null,
                warehouseId = requireNonNull(warehouseId, WarehouseMemberErrorCode.INVALID_WAREHOUSE_ID),
                principalId = requireNonNull(principalId, WarehouseMemberErrorCode.INVALID_PRINCIPAL_ID)
            )
        }

        fun reconstitute(warehouseMemberId: Long, warehouseId: Long, principalId: UUID): WarehouseMember {
            return WarehouseMember(warehouseMemberId, warehouseId, principalId)
        }
    }
}
