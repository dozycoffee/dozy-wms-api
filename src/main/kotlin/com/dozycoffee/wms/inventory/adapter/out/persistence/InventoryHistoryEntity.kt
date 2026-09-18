package com.dozycoffee.wms.inventory.adapter.out.persistence

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.global.persistence.CommonCodes
import com.dozycoffee.wms.inventory.domain.enumeration.InventoryHistoryType
import com.dozycoffee.wms.inventory.domain.model.InventoryHistory
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("inventory_history")
class InventoryHistoryEntity private constructor() : BaseEntity() {

    @Id
    var inventoryHistoryId: Long? = null
        private set

    var inventoryId: Long? = null
        private set

    var historyType: String? = null
        private set

    var quantityChange: Int? = null
        private set

    var referenceId: Long? = null
        private set

    fun toDomain(): InventoryHistory {
        val domain = InventoryHistory.reconstitute(
            requireNotNull(inventoryHistoryId),
            requireNotNull(inventoryId),
            CommonCodes.fromCode(InventoryHistoryType::class.java, requireNotNull(historyType)),
            requireNotNull(quantityChange),
            requireNotNull(referenceId)
        )
        domain.copyAuditFieldsFrom(this)
        return domain
    }

    companion object {
        private const val HISTORY_TYPE_GROUP = "INVENTORY_HISTORY_TYPE"

        fun from(domain: InventoryHistory): InventoryHistoryEntity {
            val entity = InventoryHistoryEntity()
            entity.inventoryHistoryId = domain.inventoryHistoryId
            entity.inventoryId = domain.inventoryId
            entity.historyType = CommonCodes.toCode(HISTORY_TYPE_GROUP, domain.historyType)
            entity.quantityChange = domain.quantityChange
            entity.referenceId = domain.referenceId
            return entity
        }
    }
}
