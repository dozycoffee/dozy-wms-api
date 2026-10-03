package com.dozycoffee.wms.warehouse_member.adapter.out.persistence

import com.dozycoffee.wms.warehouse_member.application.port.out.WarehouseMemberRepository
import com.dozycoffee.wms.warehouse_member.domain.model.WarehouseMember
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toSet
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class WarehouseMemberPersistenceAdapter(
    private val warehouseMemberR2dbcRepository: WarehouseMemberR2dbcRepository
) : WarehouseMemberRepository {

    override suspend fun save(warehouseMember: WarehouseMember): WarehouseMember {
        return warehouseMemberR2dbcRepository.save(WarehouseMemberEntity.from(warehouseMember)).toDomain()
    }

    override suspend fun findByWarehouseIdAndPrincipalId(warehouseId: Long, principalId: UUID): WarehouseMember? {
        return warehouseMemberR2dbcRepository.findByWarehouseIdAndPrincipalId(warehouseId, principalId.toString())
            ?.toDomain()
    }

    override fun findAllByWarehouseId(warehouseId: Long): Flow<WarehouseMember> {
        return warehouseMemberR2dbcRepository.findAllByWarehouseId(warehouseId).map { it.toDomain() }
    }

    override suspend fun findWarehouseIdsByPrincipalId(principalId: UUID): Set<Long> {
        return warehouseMemberR2dbcRepository.findWarehouseIdsByPrincipalId(principalId.toString()).toSet()
    }

    override suspend fun delete(warehouseMember: WarehouseMember) {
        warehouseMemberR2dbcRepository.deleteById(requireNotNull(warehouseMember.warehouseMemberId))
    }
}
