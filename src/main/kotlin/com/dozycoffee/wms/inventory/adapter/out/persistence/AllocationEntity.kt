package com.dozycoffee.wms.inventory.adapter.out.persistence

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.global.persistence.CommonCodes
import com.dozycoffee.wms.inventory.domain.enumeration.AllocationReferenceType
import com.dozycoffee.wms.inventory.domain.enumeration.AllocationStatus
import com.dozycoffee.wms.inventory.domain.model.Allocation
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("allocation")
class AllocationEntity private constructor() : BaseEntity() {

    @Id
    var allocationId: Long? = null
        private set

    var inventoryId: Long? = null
        private set

    var referenceType: String? = null
        private set

    var referenceId: Long? = null
        private set

    var quantity: Int? = null
        private set

    var status: String? = null
        private set

    fun toDomain(): Allocation {
        return Allocation.reconstitute(
            requireNotNull(allocationId),
            requireNotNull(inventoryId),
            CommonCodes.fromCode(AllocationReferenceType::class.java, requireNotNull(referenceType)),
            requireNotNull(referenceId),
            requireNotNull(quantity),
            CommonCodes.fromCode(AllocationStatus::class.java, requireNotNull(status))
        )
    }

    companion object {
        private const val REFERENCE_TYPE_GROUP = "ALLOCATION_REFERENCE_TYPE"
        private const val STATUS_GROUP = "ALLOCATION_STATUS"

        fun from(domain: Allocation): AllocationEntity {
            val entity = AllocationEntity()
            entity.allocationId = domain.allocationId
            entity.inventoryId = domain.inventoryId
            entity.referenceType = CommonCodes.toCode(REFERENCE_TYPE_GROUP, domain.referenceType)
            entity.referenceId = domain.referenceId
            entity.quantity = domain.quantity
            entity.status = CommonCodes.toCode(STATUS_GROUP, domain.status)
            return entity
        }
    }
}
