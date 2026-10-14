package com.dozycoffee.wms.outbound.adapter.out.persistence

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.outbound.domain.model.OutboundAllocation
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate

@Table("outbound_allocation")
class OutboundAllocationEntity private constructor() : BaseEntity() {

    @Id
    var outboundAllocationId: Long? = null
        private set

    var outboundItemId: Long? = null
        private set

    var externalInventoryId: Long? = null
        private set

    var lotNumber: String? = null
        private set

    var expirationDate: LocalDate? = null
        private set

    var quantity: Int = 0
        private set

    var pickedQuantity: Int? = null
        private set

    fun toDomain(): OutboundAllocation {
        return OutboundAllocation.reconstitute(
            requireNotNull(outboundAllocationId),
            requireNotNull(outboundItemId),
            requireNotNull(externalInventoryId),
            requireNotNull(lotNumber),
            expirationDate,
            quantity,
            pickedQuantity
        )
    }

    companion object {
        fun from(domain: OutboundAllocation): OutboundAllocationEntity {
            val entity = OutboundAllocationEntity()
            entity.outboundAllocationId = domain.outboundAllocationId
            entity.outboundItemId = domain.outboundItemId
            entity.externalInventoryId = domain.inventoryId
            entity.lotNumber = domain.lotNumber
            entity.expirationDate = domain.expirationDate
            entity.quantity = domain.quantity
            entity.pickedQuantity = domain.pickedQuantity
            return entity
        }
    }
}
