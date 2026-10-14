package com.dozycoffee.wms.location_stock.fixture

import com.dozycoffee.wms.location_stock.domain.enumeration.StockQualityStatus
import com.dozycoffee.wms.location_stock.domain.model.LocationStock

class LocationStockTestBuilder {

    private var locationStockId: Long? = null
    private var locationId: Long? = 1L
    private var productId: Long? = 1L
    private var lotNumber: String? = "LOT-001"
    private var qualityStatus: StockQualityStatus = StockQualityStatus.NORMAL
    private var quantity: Int = 10
    private var heldQuantity: Int = 0

    companion object {
        fun locationStock(): LocationStockTestBuilder = LocationStockTestBuilder()
    }

    fun locationStockId(locationStockId: Long?): LocationStockTestBuilder {
        this.locationStockId = locationStockId
        return this
    }

    fun locationId(locationId: Long?): LocationStockTestBuilder {
        this.locationId = locationId
        return this
    }

    fun productId(productId: Long?): LocationStockTestBuilder {
        this.productId = productId
        return this
    }

    fun lotNumber(lotNumber: String?): LocationStockTestBuilder {
        this.lotNumber = lotNumber
        return this
    }

    fun qualityStatus(qualityStatus: StockQualityStatus): LocationStockTestBuilder {
        this.qualityStatus = qualityStatus
        return this
    }

    fun quantity(quantity: Int): LocationStockTestBuilder {
        this.quantity = quantity
        return this
    }

    fun heldQuantity(heldQuantity: Int): LocationStockTestBuilder {
        this.heldQuantity = heldQuantity
        return this
    }

    fun build(): LocationStock {
        val id: Long? = locationStockId
        if (id != null) {
            return LocationStock.reconstitute(
                id,
                requireNotNull(locationId) { "locationId는 재구성 시 필수입니다." },
                requireNotNull(productId) { "productId는 재구성 시 필수입니다." },
                requireNotNull(lotNumber) { "lotNumber는 재구성 시 필수입니다." },
                qualityStatus,
                quantity,
                heldQuantity
            )
        }
        return LocationStock.create(locationId, productId, lotNumber, qualityStatus, quantity)
    }
}
