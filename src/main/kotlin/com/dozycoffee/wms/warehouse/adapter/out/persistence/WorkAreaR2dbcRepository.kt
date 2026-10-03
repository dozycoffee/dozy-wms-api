package com.dozycoffee.wms.warehouse.adapter.out.persistence

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface WorkAreaR2dbcRepository : CoroutineCrudRepository<WorkAreaEntity, Long> {
    suspend fun findByWarehouseIdAndAreaCode(warehouseId: Long, areaCode: String): WorkAreaEntity?
}
