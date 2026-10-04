package com.dozycoffee.wms.inbound.adapter.out.persistence

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.inbound.domain.enumeration.InspectionStatus
import com.dozycoffee.wms.inbound.domain.model.InboundItem
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate

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

    var expectedLotNumber: String? = null
        private set

    var expectedExpirationDate: LocalDate? = null
        private set

    var actualQuantity: Int? = null
        private set

    var inspectionStatus: String? = null
        private set

    fun toDomain(): InboundItem {
        return InboundItem.reconstitute(
            requireNotNull(inboundItemId),
            requireNotNull(inboundId),
            requireNotNull(productId),
            requireNotNull(zoneId),
            expectedQuantity,
            expectedLotNumber,
            expectedExpirationDate,
            actualQuantity,
            InspectionStatus.valueOf(requireNotNull(inspectionStatus))
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
            entity.expectedLotNumber = domain.expectedLotNumber
            entity.expectedExpirationDate = domain.expectedExpirationDate
            entity.actualQuantity = domain.actualQuantity
            entity.inspectionStatus = domain.inspectionStatus.name
            return entity
        }
    }
}
