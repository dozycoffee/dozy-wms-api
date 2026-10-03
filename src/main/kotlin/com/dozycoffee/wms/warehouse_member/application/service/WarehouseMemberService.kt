package com.dozycoffee.wms.warehouse_member.application.service

import com.dozycoffee.wms.warehouse.application.port.`in`.GetWarehouseUseCase
import com.dozycoffee.wms.warehouse_member.application.port.`in`.AssignWarehouseMemberUseCase
import com.dozycoffee.wms.warehouse_member.application.port.`in`.GetWarehouseMemberUseCase
import com.dozycoffee.wms.warehouse_member.application.port.`in`.RemoveWarehouseMemberUseCase
import com.dozycoffee.wms.warehouse_member.application.port.`in`.result.WarehouseMemberResult
import com.dozycoffee.wms.warehouse_member.application.port.out.WarehouseMemberRepository
import com.dozycoffee.wms.warehouse_member.domain.exception.WarehouseMemberNotFoundException
import com.dozycoffee.wms.warehouse_member.domain.model.WarehouseMember
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class WarehouseMemberService(
    private val warehouseMemberRepository: WarehouseMemberRepository,
    private val getWarehouseUseCase: GetWarehouseUseCase
) : AssignWarehouseMemberUseCase, RemoveWarehouseMemberUseCase, GetWarehouseMemberUseCase {

    @Transactional
    override suspend fun assign(warehouseId: Long, principalId: UUID): WarehouseMemberResult {
        getWarehouseUseCase.getById(warehouseId).awaitSingle()
        val member = warehouseMemberRepository.findByWarehouseIdAndPrincipalId(warehouseId, principalId)
            ?: warehouseMemberRepository.save(WarehouseMember.create(warehouseId, principalId))
        return WarehouseMemberResult.from(member)
    }

    @Transactional
    override suspend fun remove(warehouseId: Long, principalId: UUID) {
        val member = warehouseMemberRepository.findByWarehouseIdAndPrincipalId(warehouseId, principalId)
            ?: throw WarehouseMemberNotFoundException()
        warehouseMemberRepository.delete(member)
    }

    @Transactional(readOnly = true)
    override fun getAllByWarehouse(warehouseId: Long): Flow<WarehouseMemberResult> {
        return warehouseMemberRepository.findAllByWarehouseId(warehouseId).map { WarehouseMemberResult.from(it) }
    }
}
