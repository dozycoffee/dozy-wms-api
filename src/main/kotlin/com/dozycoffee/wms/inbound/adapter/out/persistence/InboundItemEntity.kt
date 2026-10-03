package com.dozycoffee.wms.inbound.adapter.out.persistence

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.inbound.domain.enumeration.InspectionResult
import com.dozycoffee.wms.inbound.domain.model.InboundItem
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("inbound_item")
class InboundItemEntity private constructor() : BaseEntity() {

    @Id
    var inboundItemId: Long? = null
        private set

    var inboundId: Long? = null
        private set

    var productId: Long? = null
        private set

    var zoneId: Long? = null
        private set

    var expectedQuantity: Int = 0
        private set

    var actualQuantity: Int? = null
        private set

    var inspectionResult: String? = null
        private set

    fun toDomain(): InboundItem {
        return InboundItem.reconstitute(
            requireNotNull(inboundItemId),
            requireNotNull(inboundId),
            requireNotNull(productId),
            requireNotNull(zoneId),
            expectedQuantity,
            actualQuantity,
            InspectionResult.valueOf(requireNotNull(inspectionResult))
        )
    }

    companion object {
        fun from(domain: InboundItem): InboundItemEntity {
            val entity = InboundItemEntity()
            entity.inboundItemId = domain.inboundItemId
            entity.inboundId = domain.inboundId
            entity.productId = domain.productId
            entity.zoneId = domain.zoneId
            entity.expectedQuantity = domain.expectedQuantity
            entity.actualQuantity = domain.actualQuantity
            entity.inspectionResult = domain.inspectionResult.name
            return entity
        }
    }
}
