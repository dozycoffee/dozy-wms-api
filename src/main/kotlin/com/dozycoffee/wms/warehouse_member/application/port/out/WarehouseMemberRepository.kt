package com.dozycoffee.wms.warehouse_member.application.port.out

import com.dozycoffee.wms.warehouse_member.domain.model.WarehouseMember
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface WarehouseMemberRepository {
    suspend fun save(warehouseMember: WarehouseMember): WarehouseMember
    suspend fun findByWarehouseIdAndPrincipalId(warehouseId: Long, principalId: UUID): WarehouseMember?
    fun findAllByWarehouseId(warehouseId: Long): Flow<WarehouseMember>
    suspend fun findWarehouseIdsByPrincipalId(principalId: UUID): Set<Long>
    suspend fun delete(warehouseMember: WarehouseMember)
}
