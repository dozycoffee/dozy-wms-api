package com.dozycoffee.wms.warehouse_member.adapter.out.persistence

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.warehouse_member.domain.model.WarehouseMember
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("warehouse_member")
class WarehouseMemberEntity private constructor() : BaseEntity() {

    @Id
    var warehouseMemberId: Long? = null
        private set

    var warehouseId: Long? = null
        private set

    var principalId: String? = null
        private set

    fun toDomain(): WarehouseMember {
        return WarehouseMember.reconstitute(
            requireNotNull(warehouseMemberId),
            requireNotNull(warehouseId),
            UUID.fromString(requireNotNull(principalId))
        )
    }

    companion object {
        fun from(domain: WarehouseMember): WarehouseMemberEntity {
            val entity = WarehouseMemberEntity()
            entity.warehouseMemberId = domain.warehouseMemberId
            entity.warehouseId = domain.warehouseId
            entity.principalId = domain.principalId.toString()
            return entity
        }
    }
}
