package com.dozycoffee.wms.inventory_call.application.service

import com.dozycoffee.wms.global.persistence.translatingDuplicateKey
import com.dozycoffee.wms.inventory_call.application.port.`in`.DiscardInventoryCallUseCase
import com.dozycoffee.wms.inventory_call.application.port.`in`.GetInventoryCallUseCase
import com.dozycoffee.wms.inventory_call.application.port.`in`.RecordInventoryCallUseCase
import com.dozycoffee.wms.inventory_call.application.port.`in`.RequeueInventoryCallUseCase
import com.dozycoffee.wms.inventory_call.application.port.`in`.command.RecordInventoryCallCommand
import com.dozycoffee.wms.inventory_call.application.port.`in`.result.InventoryCallResult
import com.dozycoffee.wms.inventory_call.application.port.out.InventoryCallRepository
import com.dozycoffee.wms.inventory_call.domain.enumeration.InventoryCallStatus
import com.dozycoffee.wms.inventory_call.domain.exception.InventoryCallKeyConflictException
import com.dozycoffee.wms.inventory_call.domain.exception.InventoryCallNotFoundException
import com.dozycoffee.wms.inventory_call.domain.model.InventoryCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class InventoryCallService(
    private val inventoryCallRepository: InventoryCallRepository,
    private val clock: Clock
) : RecordInventoryCallUseCase,
    GetInventoryCallUseCase,
    RequeueInventoryCallUseCase,
    DiscardInventoryCallUseCase {

    @Transactional
    override suspend fun record(command: RecordInventoryCallCommand): InventoryCallResult {
        val call = InventoryCall.create(
            command.callType,
            command.warehouseId,
            command.idempotencyKey,
            command.requestPath,
            command.requestBody,
            LocalDateTime.now(clock)
        )
        val existing: InventoryCall? = inventoryCallRepository.findByIdempotencyKey(call.idempotencyKey)
        if (existing != null) {
            if (!existing.hasSameRequest(call.callType, call.warehouseId, call.requestPath, call.requestBody)) {
                throw InventoryCallKeyConflictException()
            }
            return InventoryCallResult.from(existing)
        }
        val saved = translatingDuplicateKey({ InventoryCallKeyConflictException() }) {
            inventoryCallRepository.save(call)
        }
        return InventoryCallResult.from(saved)
    }

    @Transactional(readOnly = true)
    override suspend fun getById(inventoryCallId: Long): InventoryCallResult {
        return InventoryCallResult.from(findOrThrow(inventoryCallId))
    }

    @Transactional(readOnly = true)
    override fun getAll(status: InventoryCallStatus?, limit: Int): Flow<InventoryCallResult> {
        return inventoryCallRepository.findAll(status, limit).map { InventoryCallResult.from(it) }
    }

    @Transactional
    override suspend fun requeue(inventoryCallId: Long): InventoryCallResult {
        val call = findOrThrow(inventoryCallId)
        call.requeue(LocalDateTime.now(clock))
        return InventoryCallResult.from(inventoryCallRepository.save(call))
    }

    @Transactional
    override suspend fun discard(inventoryCallId: Long): InventoryCallResult {
        val call = findOrThrow(inventoryCallId)
        call.discard()
        return InventoryCallResult.from(inventoryCallRepository.save(call))
    }

    private suspend fun findOrThrow(inventoryCallId: Long): InventoryCall {
        return inventoryCallRepository.findById(inventoryCallId) ?: throw InventoryCallNotFoundException()
    }
}
