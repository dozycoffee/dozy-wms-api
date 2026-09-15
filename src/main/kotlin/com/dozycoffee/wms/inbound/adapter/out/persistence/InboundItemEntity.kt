package com.dozycoffee.wms.inbound.adapter.out.persistence

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.global.persistence.CommonCodes
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
            CommonCodes.fromCode(InspectionResult::class.java, requireNotNull(inspectionResult))
        )
    }

    companion object {
        private const val INSPECTION_RESULT_GROUP = "INBOUND_ITEM_INSPECTION_RESULT"

        fun from(domain: InboundItem): InboundItemEntity {
            val entity = InboundItemEntity()
            entity.inboundItemId = domain.inboundItemId
            entity.inboundId = domain.inboundId
            entity.productId = domain.productId
            entity.zoneId = domain.zoneId
            entity.expectedQuantity = domain.expectedQuantity
            entity.actualQuantity = domain.actualQuantity
            entity.inspectionResult = CommonCodes.toCode(INSPECTION_RESULT_GROUP, domain.inspectionResult)
            return entity
        }
    }
}
