package com.dozycoffee.wms.inventory.fixture

import com.dozycoffee.wms.inventory.domain.enumeration.InventoryHistoryType
import com.dozycoffee.wms.inventory.domain.model.InventoryHistory

class InventoryHistoryTestBuilder {

    private var inventoryHistoryId: Long? = null
    private var inventoryId: Long? = 1L
    private var historyType: InventoryHistoryType? = InventoryHistoryType.INBOUND
    private var quantityChange: Int = 10
    private var referenceId: Long? = 1L

    companion object {
        fun inventoryHistory(): InventoryHistoryTestBuilder = InventoryHistoryTestBuilder()
    }

    fun inventoryHistoryId(inventoryHistoryId: Long?): InventoryHistoryTestBuilder {
        this.inventoryHistoryId = inventoryHistoryId
        return this
    }

    fun inventoryId(inventoryId: Long?): InventoryHistoryTestBuilder {
        this.inventoryId = inventoryId
        return this
    }

    fun historyType(historyType: InventoryHistoryType?): InventoryHistoryTestBuilder {
        this.historyType = historyType
        return this
    }

    fun quantityChange(quantityChange: Int): InventoryHistoryTestBuilder {
        this.quantityChange = quantityChange
        return this
    }

    fun referenceId(referenceId: Long?): InventoryHistoryTestBuilder {
        this.referenceId = referenceId
        return this
    }

    fun build(): InventoryHistory {
        val id: Long? = inventoryHistoryId
        if (id != null) {
            return InventoryHistory.reconstitute(
                inventoryHistoryId = id,
                inventoryId = requireNotNull(inventoryId) { "inventoryId는 재구성 시 필수입니다." },
                historyType = requireNotNull(historyType) { "historyType은 재구성 시 필수입니다." },
                quantityChange = quantityChange,
                referenceId = requireNotNull(referenceId) { "referenceId는 재구성 시 필수입니다." }
            )
        }
        return InventoryHistory.create(
            inventoryId = inventoryId,
            historyType = historyType,
            quantityChange = quantityChange,
            referenceId = referenceId
        )
    }
}
