package com.dozycoffee.wms.warehouse_member.adapter.out.persistence

import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface WarehouseMemberR2dbcRepository : CoroutineCrudRepository<WarehouseMemberEntity, Long> {

    suspend fun findByWarehouseIdAndPrincipalId(warehouseId: Long, principalId: String): WarehouseMemberEntity?

    fun findAllByWarehouseId(warehouseId: Long): Flow<WarehouseMemberEntity>

    @Query("SELECT warehouse_id FROM warehouse_member WHERE principal_id = :principalId")
    fun findWarehouseIdsByPrincipalId(principalId: String): Flow<Long>
}
