package com.dozycoffee.wms.inventory.adapter.out.persistence

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface InventoryHistoryR2dbcRepository : CoroutineCrudRepository<InventoryHistoryEntity, Long>
