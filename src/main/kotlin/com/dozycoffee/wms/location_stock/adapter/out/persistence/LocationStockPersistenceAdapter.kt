package com.dozycoffee.wms.location_stock.adapter.out.persistence

import com.dozycoffee.wms.location_stock.application.port.out.LocationStockRepository
import com.dozycoffee.wms.location_stock.domain.enumeration.StockQualityStatus
import com.dozycoffee.wms.location_stock.domain.model.LocationStock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Component

@Component
class LocationStockPersistenceAdapter(
    private val locationStockR2dbcRepository: LocationStockR2dbcRepository
) : LocationStockRepository {

    override suspend fun save(locationStock: LocationStock): LocationStock {
        val entity = LocationStockEntity.from(locationStock)
        val locationStockId = locationStock.locationStockId
        if (locationStockId != null) {
            locationStockR2dbcRepository.findById(locationStockId)?.let { entity.copyAuditFieldsFrom(it) }
        }
        return locationStockR2dbcRepository.save(entity).toDomain()
    }

    override suspend fun findForUpdate(
        locationId: Long,
        productId: Long,
        lotNumber: String,
        qualityStatus: StockQualityStatus
    ): LocationStock? {
        return locationStockR2dbcRepository
            .findByKeyForUpdate(locationId, productId, lotNumber, qualityStatus.name)
            ?.toDomain()
    }

    override fun findAllByLocationId(locationId: Long): Flow<LocationStock> {
        return locationStockR2dbcRepository.findAllByLocationId(locationId).map { it.toDomain() }
    }
}
