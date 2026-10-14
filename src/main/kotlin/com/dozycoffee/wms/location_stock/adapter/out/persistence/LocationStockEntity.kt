package com.dozycoffee.wms.location_stock.adapter.out.persistence

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.location_stock.domain.enumeration.StockQualityStatus
import com.dozycoffee.wms.location_stock.domain.model.LocationStock
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("location_stock")
class LocationStockEntity private constructor() : BaseEntity() {

    @Id
    var locationStockId: Long? = null
        private set

    var locationId: Long? = null
        private set

    var productId: Long? = null
        private set

    var lotNumber: String? = null
        private set

    var qualityStatus: String? = null
        private set

    var quantity: Int? = null
        private set

    var heldQuantity: Int = 0
        private set

    fun toDomain(): LocationStock {
        return LocationStock.reconstitute(
            requireNotNull(locationStockId),
            requireNotNull(locationId),
            requireNotNull(productId),
            requireNotNull(lotNumber),
            StockQualityStatus.valueOf(requireNotNull(qualityStatus)),
            requireNotNull(quantity),
            heldQuantity
        )
    }

    companion object {
        fun from(domain: LocationStock): LocationStockEntity {
            val entity = LocationStockEntity()
            entity.locationStockId = domain.locationStockId
            entity.locationId = domain.locationId
            entity.productId = domain.productId
            entity.lotNumber = domain.lotNumber
            entity.qualityStatus = domain.qualityStatus.name
            entity.quantity = domain.quantity
            entity.heldQuantity = domain.heldQuantity
            return entity
        }
    }
}
