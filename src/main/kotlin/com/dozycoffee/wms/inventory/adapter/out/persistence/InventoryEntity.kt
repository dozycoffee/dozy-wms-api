package com.dozycoffee.wms.inventory.adapter.out.persistence

import com.dozycoffee.wms.global.common.SoftDeletableEntity
import com.dozycoffee.wms.global.persistence.CommonCodes
import com.dozycoffee.wms.inventory.domain.enumeration.QualityStatus
import com.dozycoffee.wms.inventory.domain.model.Inventory
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("inventory")
class InventoryEntity private constructor() : SoftDeletableEntity() {

    @Id
    var inventoryId: Long? = null
        private set

    var productId: Long? = null
        private set

    var lotId: Long? = null
        private set

    var locationId: Long? = null
        private set

    var quantity: Int? = null
        private set

    var allocatedQuantity: Int? = null
        private set

    var qualityStatus: String? = null
        private set

    fun toDomain(): Inventory {
        return Inventory.reconstitute(
            requireNotNull(inventoryId),
            requireNotNull(productId),
            requireNotNull(lotId),
            requireNotNull(locationId),
            requireNotNull(quantity),
            requireNotNull(allocatedQuantity),
            CommonCodes.fromCode(QualityStatus::class.java, requireNotNull(qualityStatus))
        )
    }

    companion object {
        private const val QUALITY_STATUS_GROUP = "QUALITY_STATUS"

        fun from(domain: Inventory): InventoryEntity {
            val entity = InventoryEntity()
            entity.inventoryId = domain.inventoryId
            entity.productId = domain.productId
            entity.lotId = domain.lotId
            entity.locationId = domain.locationId
            entity.quantity = domain.quantity
            entity.allocatedQuantity = domain.allocatedQuantity
            entity.qualityStatus = CommonCodes.toCode(QUALITY_STATUS_GROUP, domain.qualityStatus)
            if (domain.isDeleted()) {
                entity.softDelete(requireNotNull(domain.deletedBy))
            }
            return entity
        }
    }
}
