package com.dozycoffee.wms.inventory.application.service

import com.dozycoffee.wms.inventory.application.port.`in`.GetInventoryUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.MarkInventoryDefectiveUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.MarkInventoryDisposalScheduledUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.RegisterInventoryUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.command.RegisterInventoryCommand
import com.dozycoffee.wms.inventory.application.port.`in`.result.InventoryResult
import com.dozycoffee.wms.inventory.application.port.out.InventoryRepository
import com.dozycoffee.wms.inventory.application.port.out.LotRepository
import com.dozycoffee.wms.inventory.domain.enumeration.QualityStatus
import com.dozycoffee.wms.inventory.domain.exception.InventoryNotFoundException
import com.dozycoffee.wms.inventory.domain.exception.LotNotFoundException
import com.dozycoffee.wms.inventory.domain.model.Inventory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class InventoryService(
    private val inventoryRepository: InventoryRepository,
    private val lotRepository: LotRepository
) : RegisterInventoryUseCase, GetInventoryUseCase, MarkInventoryDefectiveUseCase, MarkInventoryDisposalScheduledUseCase {

    @Transactional
    override suspend fun register(command: RegisterInventoryCommand): InventoryResult {
        val lot = lotRepository.findById(command.lotId) ?: throw LotNotFoundException()
        val inventory = Inventory.create(
            productId = lot.productId,
            lotId = lot.lotId,
            locationId = command.locationId,
            quantity = command.quantity
        )
        return InventoryResult.from(inventoryRepository.save(inventory))
    }

    @Transactional(readOnly = true)
    override suspend fun getById(inventoryId: Long): InventoryResult {
        return InventoryResult.from(findInventoryOrThrow(inventoryId))
    }

    @Transactional(readOnly = true)
    override fun getAll(locationId: Long?, productId: Long?, qualityStatus: QualityStatus?): Flow<InventoryResult> {
        return inventoryRepository.findAll(locationId, productId, qualityStatus).map { InventoryResult.from(it) }
    }

    @Transactional
    override suspend fun markDefective(inventoryId: Long): InventoryResult {
        val inventory = findInventoryOrThrow(inventoryId)
        inventory.markDefective()
        return InventoryResult.from(inventoryRepository.save(inventory))
    }

    @Transactional
    override suspend fun markDisposalScheduled(inventoryId: Long): InventoryResult {
        val inventory = findInventoryOrThrow(inventoryId)
        inventory.markDisposalScheduled()
        return InventoryResult.from(inventoryRepository.save(inventory))
    }

    private suspend fun findInventoryOrThrow(inventoryId: Long): Inventory {
        return inventoryRepository.findById(inventoryId) ?: throw InventoryNotFoundException()
    }
}
