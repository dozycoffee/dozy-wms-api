package com.dozycoffee.wms.warehouse.adapter.out.persistence

import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface LocationR2dbcRepository : CoroutineCrudRepository<LocationEntity, Long> {
    fun findByZoneId(zoneId: Long): Flow<LocationEntity>
}
