package com.dozycoffee.wms.inventory.application.service

import com.dozycoffee.wms.inventory.application.port.`in`.FulfillAllocationUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.HoldInventoryUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.ReleaseAllocationUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.command.HoldInventoryCommand
import com.dozycoffee.wms.inventory.application.port.`in`.result.AllocationResult
import com.dozycoffee.wms.inventory.application.port.out.AllocationRepository
import com.dozycoffee.wms.inventory.application.port.out.InventoryRepository
import com.dozycoffee.wms.inventory.domain.exception.AllocationNotFoundException
import com.dozycoffee.wms.inventory.domain.exception.InventoryNotFoundException
import com.dozycoffee.wms.inventory.domain.model.Allocation
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AllocationService(
    private val allocationRepository: AllocationRepository,
    private val inventoryRepository: InventoryRepository
) : HoldInventoryUseCase, ReleaseAllocationUseCase, FulfillAllocationUseCase {

    /**
     * ADR-0008 멱등성 처리: Allocation을 먼저 insert하고, 그게 성공했을 때만 Inventory.hold()를 반영한다.
     * 같은 참조(inventoryId, referenceType, referenceId)가 중복 요청되면 idempotency_key 유니크 제약에 걸려
     * DataIntegrityViolationException이 발생하는데(MySQL 유니크 위반은 DuplicateKeyException이 아닌 상위
     * 타입으로 번역된다 — AllocationPersistenceAdapterTest로 실측 확인), 이 경우 Inventory는 이미 첫 번째
     * 요청에서 갱신되었으므로 다시 건드리지 않고 기존 HELD Allocation을 그대로 반환한다.
     */
    @Transactional
    override suspend fun hold(command: HoldInventoryCommand): AllocationResult {
        val allocation = Allocation.create(
            inventoryId = command.inventoryId,
            referenceType = command.referenceType,
            referenceId = command.referenceId,
            quantity = command.quantity
        )
        val saved: Allocation =
            try {
                allocationRepository.save(allocation)
            } catch (e: DataIntegrityViolationException) {
                return AllocationResult.from(
                    allocationRepository.findHeld(command.inventoryId, command.referenceType, command.referenceId)
                        ?: throw e
                )
            }

        val inventory = inventoryRepository.findById(command.inventoryId) ?: throw InventoryNotFoundException()
        inventory.hold(command.quantity)
        inventoryRepository.save(inventory)

        return AllocationResult.from(saved)
    }

    @Transactional
    override suspend fun release(allocationId: Long): AllocationResult {
        val allocation = findAllocationOrThrow(allocationId)
        allocation.release()
        val saved = allocationRepository.save(allocation)

        val inventory = inventoryRepository.findById(allocation.inventoryId) ?: throw InventoryNotFoundException()
        inventory.releaseHold(allocation.quantity)
        inventoryRepository.save(inventory)

        return AllocationResult.from(saved)
    }

    @Transactional
    override suspend fun fulfill(allocationId: Long): AllocationResult {
        val allocation = findAllocationOrThrow(allocationId)
        allocation.fulfill()
        val saved = allocationRepository.save(allocation)

        val inventory = inventoryRepository.findById(allocation.inventoryId) ?: throw InventoryNotFoundException()
        inventory.fulfillHold(allocation.quantity)
        inventoryRepository.save(inventory)

        return AllocationResult.from(saved)
    }

    private suspend fun findAllocationOrThrow(allocationId: Long): Allocation {
        return allocationRepository.findById(allocationId) ?: throw AllocationNotFoundException()
    }
}
