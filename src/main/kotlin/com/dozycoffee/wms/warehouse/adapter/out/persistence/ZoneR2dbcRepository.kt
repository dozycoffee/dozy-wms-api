package com.dozycoffee.wms.warehouse.adapter.out.persistence

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface ZoneR2dbcRepository : CoroutineCrudRepository<ZoneEntity, Long> {
    suspend fun findByWarehouseIdAndZoneCode(warehouseId: Long, zoneCode: String): ZoneEntity?
}
