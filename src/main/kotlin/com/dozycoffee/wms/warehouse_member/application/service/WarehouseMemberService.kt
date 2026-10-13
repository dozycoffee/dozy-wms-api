package com.dozycoffee.wms.warehouse_member.application.service

import com.dozycoffee.wms.global.event.UuidV7
import com.dozycoffee.wms.global.persistence.translatingDuplicateKey
import com.dozycoffee.wms.warehouse.application.port.`in`.GetWarehouseUseCase
import com.dozycoffee.wms.warehouse_member.application.port.`in`.AssignWarehouseMemberUseCase
import com.dozycoffee.wms.warehouse_member.application.port.`in`.GetWarehouseMemberUseCase
import com.dozycoffee.wms.warehouse_member.application.port.`in`.RemoveWarehouseMemberUseCase
import com.dozycoffee.wms.warehouse_member.application.port.`in`.result.WarehouseMemberResult
import com.dozycoffee.wms.warehouse_member.application.port.out.WarehouseAccessEventRepository
import com.dozycoffee.wms.warehouse_member.application.port.out.WarehouseMemberRepository
import com.dozycoffee.wms.warehouse_member.domain.exception.WarehouseMemberAlreadyAssignedException
import com.dozycoffee.wms.warehouse_member.domain.exception.WarehouseMemberNotFoundException
import com.dozycoffee.wms.warehouse_member.domain.model.WarehouseMember
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

@Service
class WarehouseMemberService(
    private val warehouseMemberRepository: WarehouseMemberRepository,
    private val warehouseAccessEventRepository: WarehouseAccessEventRepository,
    private val getWarehouseUseCase: GetWarehouseUseCase,
    private val clock: Clock
) : AssignWarehouseMemberUseCase, RemoveWarehouseMemberUseCase, GetWarehouseMemberUseCase {

    @Transactional
    override suspend fun assign(warehouseId: Long, principalId: UUID): WarehouseMemberResult {
        getWarehouseUseCase.getById(warehouseId)
        val existing = warehouseMemberRepository.findByWarehouseIdAndPrincipalId(warehouseId, principalId)
        if (existing != null) return WarehouseMemberResult.from(existing)
        val member = translatingDuplicateKey({ WarehouseMemberAlreadyAssignedException() }) {
            warehouseMemberRepository.save(WarehouseMember.create(warehouseId, principalId))
        }
        markAccessChanged(principalId)
        return WarehouseMemberResult.from(member)
    }

    @Transactional
    override suspend fun remove(warehouseId: Long, principalId: UUID) {
        val member = warehouseMemberRepository.findByWarehouseIdAndPrincipalId(warehouseId, principalId)
            ?: throw WarehouseMemberNotFoundException()
        warehouseMemberRepository.delete(member)
        markAccessChanged(principalId)
    }

    @Transactional(readOnly = true)
    override fun getAllByWarehouse(warehouseId: Long): Flow<WarehouseMemberResult> {
        return warehouseMemberRepository.findAllByWarehouseId(warehouseId).map { WarehouseMemberResult.from(it) }
    }

    /** 소속이 바뀐 직원을 inventory로 보낼 발행 대기에 같은 트랜잭션으로 기록한다(ADR-0020) */
    private suspend fun markAccessChanged(principalId: UUID) {
        val now = LocalDateTime.now(clock)
        warehouseAccessEventRepository.markChanged(principalId, UuidV7.generate(clock.millis()), now, now)
    }
}
