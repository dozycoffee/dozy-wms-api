package com.dozycoffee.wms.outbound.adapter.out.persistence

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.outbound.domain.model.OutboundItem
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("outbound_item")
class OutboundItemEntity private constructor() : BaseEntity() {

    @Id
    var outboundItemId: Long? = null
        private set

    var outboundId: Long? = null
        private set

    var productId: Long? = null
        private set

    var requestedQuantity: Int = 0
        private set

    var pickedQuantity: Int? = null
        private set

    fun toDomain(): OutboundItem {
        return OutboundItem.reconstitute(
            requireNotNull(outboundItemId),
            requireNotNull(outboundId),
            requireNotNull(productId),
            requestedQuantity,
            pickedQuantity
        )
    }

    companion object {
        fun from(domain: OutboundItem): OutboundItemEntity {
            val entity = OutboundItemEntity()
            entity.outboundItemId = domain.outboundItemId
            entity.outboundId = domain.outboundId
            entity.productId = domain.productId
            entity.requestedQuantity = domain.requestedQuantity
            entity.pickedQuantity = domain.pickedQuantity
            return entity
        }
    }
}
