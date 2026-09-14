package com.dozycoffee.wms.inventory.fixture

import com.dozycoffee.wms.inventory.domain.enumeration.QualityStatus
import com.dozycoffee.wms.inventory.domain.model.Inventory

class InventoryTestBuilder {

    private var inventoryId: Long? = null
    private var productId: Long? = 1L
    private var lotId: Long? = 1L
    private var locationId: Long? = 1L
    private var quantity: Int = 10
    private var allocatedQuantity: Int = 0
    private var qualityStatus: QualityStatus = QualityStatus.NORMAL

    companion object {
        fun inventory(): InventoryTestBuilder = InventoryTestBuilder()
    }

    fun inventoryId(inventoryId: Long?): InventoryTestBuilder {
        this.inventoryId = inventoryId
        return this
    }

    fun productId(productId: Long?): InventoryTestBuilder {
        this.productId = productId
        return this
    }

    fun lotId(lotId: Long?): InventoryTestBuilder {
        this.lotId = lotId
        return this
    }

    fun locationId(locationId: Long?): InventoryTestBuilder {
        this.locationId = locationId
        return this
    }

    fun quantity(quantity: Int): InventoryTestBuilder {
        this.quantity = quantity
        return this
    }

    fun allocatedQuantity(allocatedQuantity: Int): InventoryTestBuilder {
        this.allocatedQuantity = allocatedQuantity
        return this
    }

    fun qualityStatus(qualityStatus: QualityStatus): InventoryTestBuilder {
        this.qualityStatus = qualityStatus
        return this
    }

    fun build(): Inventory {
        val id: Long? = inventoryId
        if (id != null) {
            return Inventory.reconstitute(
                inventoryId = id,
                productId = requireNotNull(productId) { "productId는 재구성 시 필수입니다." },
                lotId = requireNotNull(lotId) { "lotId는 재구성 시 필수입니다." },
                locationId = requireNotNull(locationId) { "locationId는 재구성 시 필수입니다." },
                quantity = quantity,
                allocatedQuantity = allocatedQuantity,
                qualityStatus = qualityStatus
            )
        }
        return Inventory.create(
            productId = productId,
            lotId = lotId,
            locationId = locationId,
            quantity = quantity
        )
    }
}
