package com.dozycoffee.wms.disposal.adapter.out.persistence

import com.dozycoffee.wms.disposal.domain.enumeration.DisposalReason
import com.dozycoffee.wms.disposal.domain.model.DisposalItem
import com.dozycoffee.wms.global.common.BaseEntity
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("disposal_item")
class DisposalItemEntity private constructor() : BaseEntity() {

    @Id
    var disposalItemId: Long? = null
        private set

    var disposalId: Long? = null
        private set

    var inventoryId: Long? = null
        private set

    var quantity: Int = 0
        private set

    var reason: String? = null
        private set

    fun toDomain(): DisposalItem {
        return DisposalItem.reconstitute(
            requireNotNull(disposalItemId),
            requireNotNull(disposalId),
            requireNotNull(inventoryId),
            quantity,
            DisposalReason.valueOf(requireNotNull(reason))
        )
    }

    companion object {
        fun from(domain: DisposalItem): DisposalItemEntity {
            val entity = DisposalItemEntity()
            entity.disposalItemId = domain.disposalItemId
            entity.disposalId = domain.disposalId
            entity.inventoryId = domain.inventoryId
            entity.quantity = domain.quantity
            entity.reason = domain.reason.name
            return entity
        }
    }
}
