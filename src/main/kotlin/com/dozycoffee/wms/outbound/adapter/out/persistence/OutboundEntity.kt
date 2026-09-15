package com.dozycoffee.wms.outbound.adapter.out.persistence

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.global.persistence.CommonCodes
import com.dozycoffee.wms.outbound.domain.enumeration.OutboundStatus
import com.dozycoffee.wms.outbound.domain.model.Outbound
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("outbound")
class OutboundEntity private constructor() : BaseEntity() {

    @Id
    var outboundId: Long? = null
        private set

    var warehouseId: Long? = null
        private set

    var status: String? = null
        private set

    fun toDomain(): Outbound {
        return Outbound.reconstitute(
            requireNotNull(outboundId),
            requireNotNull(warehouseId),
            CommonCodes.fromCode(OutboundStatus::class.java, requireNotNull(status))
        )
    }

    companion object {
        private const val STATUS_GROUP = "OUTBOUND_STATUS"

        fun from(domain: Outbound): OutboundEntity {
            val entity = OutboundEntity()
            entity.outboundId = domain.outboundId
            entity.warehouseId = domain.warehouseId
            entity.status = CommonCodes.toCode(STATUS_GROUP, domain.status)
            return entity
        }
    }
}
