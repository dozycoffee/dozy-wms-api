package com.dozycoffee.wms.outbound.adapter.out.persistence

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.outbound.domain.model.OutboundAllocationPick
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("outbound_allocation_pick")
class OutboundAllocationPickEntity private constructor() : BaseEntity() {

    @Id
    var outboundAllocationPickId: Long? = null
        private set

    var outboundAllocationId: Long? = null
        private set

    var locationStockId: Long? = null
        private set

    var quantity: Int = 0
        private set

    fun toDomain(): OutboundAllocationPick {
        return OutboundAllocationPick.reconstitute(
            requireNotNull(outboundAllocationPickId),
            requireNotNull(outboundAllocationId),
            requireNotNull(locationStockId),
            quantity
        )
    }

    companion object {
        fun from(domain: OutboundAllocationPick): OutboundAllocationPickEntity {
            val entity = OutboundAllocationPickEntity()
            entity.outboundAllocationPickId = domain.outboundAllocationPickId
            entity.outboundAllocationId = domain.outboundAllocationId
            entity.locationStockId = domain.locationStockId
            entity.quantity = domain.quantity
            return entity
        }
    }
}
