package com.dozycoffee.wms.inventory.adapter.out.persistence

import com.dozycoffee.wms.global.common.BaseEntity
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
            InventoryHistoryType.valueOf(requireNotNull(historyType)),
            requireNotNull(quantityChange),
            requireNotNull(referenceId)
        )
        domain.copyAuditFieldsFrom(this)
        return domain
    }

    companion object {
        fun from(domain: InventoryHistory): InventoryHistoryEntity {
            val entity = InventoryHistoryEntity()
            entity.inventoryHistoryId = domain.inventoryHistoryId
            entity.inventoryId = domain.inventoryId
            entity.historyType = domain.historyType.name
            entity.quantityChange = domain.quantityChange
            entity.referenceId = domain.referenceId
            return entity
        }
    }
}
