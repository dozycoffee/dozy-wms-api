package com.dozycoffee.wms.warehouse.application.port.out

import com.dozycoffee.wms.warehouse.domain.model.Location
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    suspend fun save(location: Location): Location
    suspend fun findById(locationId: Long): Location?
    fun findByZoneId(zoneId: Long): Flow<Location>
    suspend fun delete(location: Location)
}
