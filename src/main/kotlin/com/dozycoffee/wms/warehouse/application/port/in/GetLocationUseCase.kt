package com.dozycoffee.wms.warehouse.application.port.`in`

import com.dozycoffee.wms.warehouse.application.port.`in`.result.LocationResult
import kotlinx.coroutines.flow.Flow

interface GetLocationUseCase {
    suspend fun getById(locationId: Long): LocationResult
    fun getByZoneId(zoneId: Long): Flow<LocationResult>
}
