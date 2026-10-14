package com.dozycoffee.wms.location_stock.adapter.out.persistence

import com.dozycoffee.wms.location_stock.application.port.out.LocationStockRepository
import com.dozycoffee.wms.location_stock.domain.enumeration.StockQualityStatus
import com.dozycoffee.wms.location_stock.domain.model.LocationStock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Component
import java.time.LocalDateTime

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

    override suspend fun findById(locationStockId: Long): LocationStock? {
        return locationStockR2dbcRepository.findById(locationStockId)?.toDomain()
    }

    override suspend fun findByIdForUpdate(locationStockId: Long): LocationStock? {
        return locationStockR2dbcRepository.findByIdForUpdate(locationStockId)?.toDomain()
    }

    override suspend fun findAllByIds(locationStockIds: Collection<Long>): List<LocationStock> {
        if (locationStockIds.isEmpty()) return emptyList()
        return locationStockR2dbcRepository.findAllByLocationStockIdIn(locationStockIds).map { it.toDomain() }.toList()
    }

    override suspend fun findAvailableForUpdate(
        warehouseId: Long,
        productId: Long,
        lotNumber: String,
        qualityStatus: StockQualityStatus
    ): List<LocationStock> {
        return locationStockR2dbcRepository
            .findAvailableForUpdate(warehouseId, productId, lotNumber, qualityStatus.name)
            .map { it.toDomain() }
            .toList()
    }

    override suspend fun existsByProductIdAndLotNumber(productId: Long, lotNumber: String): Boolean {
        return locationStockR2dbcRepository.countByProductIdAndLotNumber(productId, lotNumber) > 0
    }

    override suspend fun existsUpdatedAfter(locationStockId: Long, since: LocalDateTime): Boolean {
        return locationStockR2dbcRepository.countUpdatedAfter(locationStockId, since) > 0
    }
}
