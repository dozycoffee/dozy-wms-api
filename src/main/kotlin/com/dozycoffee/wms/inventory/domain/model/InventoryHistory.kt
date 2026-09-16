package com.dozycoffee.wms.inventory.domain.model

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.global.error.DomainValidator.requireNonNull
import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.inventory.domain.enumeration.InventoryHistoryType
import com.dozycoffee.wms.inventory.domain.exception.InventoryHistoryErrorCode

class InventoryHistory private constructor(
    val inventoryHistoryId: Long?,
    val inventoryId: Long,
    val historyType: InventoryHistoryType,
    val quantityChange: Int,
    val referenceId: Long
) : BaseEntity() {

    companion object {

        fun create(
            inventoryId: Long?,
            historyType: InventoryHistoryType?,
            quantityChange: Int,
            referenceId: Long?
        ): InventoryHistory {
            val validInventoryId: Long = requireNonNull(inventoryId, InventoryHistoryErrorCode.INVALID_INVENTORY_ID)
            val validHistoryType: InventoryHistoryType =
                requireNonNull(historyType, InventoryHistoryErrorCode.INVALID_HISTORY_TYPE)
            val validReferenceId: Long = requireNonNull(referenceId, InventoryHistoryErrorCode.INVALID_REFERENCE_ID)
            validateQuantityChange(quantityChange)
            return InventoryHistory(
                inventoryHistoryId = null,
                inventoryId = validInventoryId,
                historyType = validHistoryType,
                quantityChange = quantityChange,
                referenceId = validReferenceId
            )
        }

        fun reconstitute(
            inventoryHistoryId: Long,
            inventoryId: Long,
            historyType: InventoryHistoryType,
            quantityChange: Int,
            referenceId: Long
        ): InventoryHistory {
            return InventoryHistory(inventoryHistoryId, inventoryId, historyType, quantityChange, referenceId)
        }

        private fun validateQuantityChange(quantityChange: Int) {
            if (quantityChange == 0) {
                throw InvalidDomainValueException(InventoryHistoryErrorCode.INVALID_QUANTITY_CHANGE)
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InventoryHistory) return false
        val id: Long? = inventoryHistoryId
        return id != null && id == other.inventoryHistoryId
    }

    override fun hashCode(): Int {
        return inventoryHistoryId?.hashCode() ?: System.identityHashCode(this)
    }
}
