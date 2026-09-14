package com.dozycoffee.wms.inventory.adapter.out.persistence

import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface AllocationR2dbcRepository : CoroutineCrudRepository<AllocationEntity, Long> {

    @Query(
        """
        SELECT * FROM allocation
        WHERE inventory_id = :inventoryId
          AND reference_type = :referenceType
          AND reference_id = :referenceId
          AND status = 'ALLOCATION_STATUS_HELD'
        """
    )
    suspend fun findHeld(inventoryId: Long, referenceType: String, referenceId: Long): AllocationEntity?
}
