package com.dozycoffee.wms.inventory_call.adapter.out.persistence

import com.dozycoffee.wms.inventory_call.application.port.out.InventoryCallRepository
import com.dozycoffee.wms.inventory_call.domain.enumeration.InventoryCallStatus
import com.dozycoffee.wms.inventory_call.domain.model.InventoryCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class InventoryCallPersistenceAdapter(
    private val inventoryCallR2dbcRepository: InventoryCallR2dbcRepository
) : InventoryCallRepository {

    override suspend fun save(inventoryCall: InventoryCall): InventoryCall {
        val entity = InventoryCallEntity.from(inventoryCall)
        val inventoryCallId = inventoryCall.inventoryCallId
        if (inventoryCallId != null) {
            inventoryCallR2dbcRepository.findById(inventoryCallId)?.let { entity.copyAuditFieldsFrom(it) }
        }
        return inventoryCallR2dbcRepository.save(entity).toDomain()
    }

    override suspend fun findById(inventoryCallId: Long): InventoryCall? {
        return inventoryCallR2dbcRepository.findById(inventoryCallId)?.toDomain()
    }

    override suspend fun findByIdempotencyKey(idempotencyKey: String): InventoryCall? {
        return inventoryCallR2dbcRepository.findByIdempotencyKey(idempotencyKey)?.toDomain()
    }

    override fun findAll(status: InventoryCallStatus?, limit: Int): Flow<InventoryCall> {
        val entities: Flow<InventoryCallEntity> =
            if (status == null) {
                inventoryCallR2dbcRepository.findRecent(limit)
            } else {
                inventoryCallR2dbcRepository.findRecentByStatus(status.name, limit)
            }
        return entities.map { it.toDomain() }
    }

    override fun findDispatchable(now: LocalDateTime, limit: Int): Flow<InventoryCall> {
        return inventoryCallR2dbcRepository.findDispatchable(now, limit).map { it.toDomain() }
    }
}
