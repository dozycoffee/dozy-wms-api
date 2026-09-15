package com.dozycoffee.wms.inbound.adapter.out.persistence

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.global.persistence.CommonCodes
import com.dozycoffee.wms.inbound.domain.enumeration.InboundStatus
import com.dozycoffee.wms.inbound.domain.model.Inbound
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate

@Table("inbound")
class InboundEntity private constructor() : BaseEntity() {

    @Id
    var inboundId: Long? = null
        private set

    var warehouseId: Long? = null
        private set

    var expectedArrivalDate: LocalDate? = null
        private set

    var status: String? = null
        private set

    fun toDomain(): Inbound {
        return Inbound.reconstitute(
            requireNotNull(inboundId),
            requireNotNull(warehouseId),
            requireNotNull(expectedArrivalDate),
            CommonCodes.fromCode(InboundStatus::class.java, requireNotNull(status))
        )
    }

    companion object {
        private const val STATUS_GROUP = "INBOUND_STATUS"

        fun from(domain: Inbound): InboundEntity {
            val entity = InboundEntity()
            entity.inboundId = domain.inboundId
            entity.warehouseId = domain.warehouseId
            entity.expectedArrivalDate = domain.expectedArrivalDate
            entity.status = CommonCodes.toCode(STATUS_GROUP, domain.status)
            return entity
        }
    }
}
