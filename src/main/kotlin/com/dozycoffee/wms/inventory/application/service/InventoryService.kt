package com.dozycoffee.wms.inventory.application.service

import com.dozycoffee.wms.global.security.CurrentAccessScopeProvider
import com.dozycoffee.wms.inventory.application.port.`in`.AdjustInventoryQuantityUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.ConfirmInventoryDisposalUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.GetInventoryHistoryUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.GetInventoryUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.InventorySortBy
import com.dozycoffee.wms.inventory.application.port.`in`.MarkInventoryDefectiveUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.MarkInventoryDisposalScheduledUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.RegisterInventoryUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.command.RegisterInventoryCommand
import com.dozycoffee.wms.inventory.application.port.`in`.result.InventoryDetailResult
import com.dozycoffee.wms.inventory.application.port.`in`.result.InventoryHistoryResult
import com.dozycoffee.wms.inventory.application.port.`in`.result.InventoryResult
import com.dozycoffee.wms.inventory.application.port.`in`.result.LotResult
import com.dozycoffee.wms.inventory.application.port.out.InventoryHistoryRepository
import com.dozycoffee.wms.inventory.application.port.out.InventoryRepository
import com.dozycoffee.wms.inventory.application.port.out.LotRepository
import com.dozycoffee.wms.inventory.domain.enumeration.InventoryHistoryType
import com.dozycoffee.wms.inventory.domain.enumeration.QualityStatus
import com.dozycoffee.wms.inventory.domain.exception.InventoryNotFoundException
import com.dozycoffee.wms.inventory.domain.exception.LotNotFoundException
import com.dozycoffee.wms.inventory.domain.model.Inventory
import com.dozycoffee.wms.inventory.domain.model.InventoryHistory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class InventoryService(
    private val inventoryRepository: InventoryRepository,
    private val lotRepository: LotRepository,
    private val inventoryHistoryRepository: InventoryHistoryRepository,
    private val currentAccessScopeProvider: CurrentAccessScopeProvider
) : RegisterInventoryUseCase,
    GetInventoryUseCase,
    MarkInventoryDefectiveUseCase,
    MarkInventoryDisposalScheduledUseCase,
    ConfirmInventoryDisposalUseCase,
    AdjustInventoryQuantityUseCase,
    GetInventoryHistoryUseCase {

    @Transactional
    override suspend fun register(command: RegisterInventoryCommand): InventoryResult {
        val lot = lotRepository.findById(command.lotId) ?: throw LotNotFoundException()
        val inventory = Inventory.create(
            productId = lot.productId,
            lotId = lot.lotId,
            locationId = command.locationId,
            quantity = command.quantity
        )
        val saved = inventoryRepository.save(inventory)
        recordHistory(saved.inventoryId, InventoryHistoryType.INBOUND, command.quantity, command.referenceId)
        return InventoryResult.from(saved)
    }

    @Transactional(readOnly = true)
    override suspend fun getById(inventoryId: Long): InventoryResult {
        return InventoryResult.from(findInventoryOrThrow(inventoryId))
    }

    @Transactional(readOnly = true)
    override suspend fun getDetailById(inventoryId: Long): InventoryDetailResult {
        val inventory = findInventoryOrThrow(inventoryId)
        val lot = lotRepository.findById(inventory.lotId) ?: throw LotNotFoundException()
        val recentHistories = inventoryHistoryRepository.findRecentByInventoryId(inventoryId, RECENT_HISTORY_LIMIT)
            .map { InventoryHistoryResult.from(it) }
            .toList()
        return InventoryDetailResult(InventoryResult.from(inventory), LotResult.from(lot), recentHistories)
    }

    @Transactional(readOnly = true)
    override fun getAll(
        locationId: Long?,
        productId: Long?,
        qualityStatus: QualityStatus?,
        sortBy: InventorySortBy?,
        warehouseIds: List<Long>?
    ): Flow<InventoryResult> = flow {
        val effectiveWarehouseIds = currentAccessScopeProvider.get().narrowWarehouseIds(warehouseIds)
        if (effectiveWarehouseIds != null && effectiveWarehouseIds.isEmpty()) return@flow
        emitAll(
            inventoryRepository.findAll(locationId, productId, qualityStatus, sortBy, effectiveWarehouseIds)
                .map { InventoryResult.from(it) }
        )
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

    /** 폐기 확정 — 대상 재고를 가용/총 수량에서 완전히 제외한다 */
    @Transactional
    override suspend fun confirmDisposal(inventoryId: Long, referenceId: Long): InventoryResult {
        val inventory = findInventoryOrThrow(inventoryId)
        val disposedQuantity = inventory.quantity
        inventory.delete(currentAccessScopeProvider.get().userId)
        val saved = inventoryRepository.save(inventory)
        recordHistory(saved.inventoryId, InventoryHistoryType.DISPOSAL, -disposedQuantity, referenceId)
        return InventoryResult.from(saved)
    }

    /** 재고 실사 확정 — 실측 수량으로 재고를 교정하고 변동분을 ADJUSTMENT 이력으로 남긴다 */
    @Transactional
    override suspend fun adjust(inventoryId: Long, newQuantity: Int, referenceId: Long): InventoryResult {
        val inventory = findInventoryOrThrow(inventoryId)
        val quantityChange = newQuantity - inventory.quantity
        inventory.adjust(newQuantity)
        val saved = inventoryRepository.save(inventory)
        if (quantityChange != 0) {
            recordHistory(saved.inventoryId, InventoryHistoryType.ADJUSTMENT, quantityChange, referenceId)
        }
        return InventoryResult.from(saved)
    }

    @Transactional(readOnly = true)
    override fun getAll(
        inventoryId: Long?,
        historyType: InventoryHistoryType?,
        from: LocalDate?,
        to: LocalDate?
    ): Flow<InventoryHistoryResult> {
        val fromDateTime = from?.atStartOfDay()
        val toDateTime = to?.plusDays(1)?.atStartOfDay()
        return inventoryHistoryRepository.findAll(inventoryId, historyType, fromDateTime, toDateTime)
            .map { InventoryHistoryResult.from(it) }
    }

    private suspend fun findInventoryOrThrow(inventoryId: Long): Inventory {
        return inventoryRepository.findById(inventoryId) ?: throw InventoryNotFoundException()
    }

    private suspend fun recordHistory(
        inventoryId: Long?,
        historyType: InventoryHistoryType,
        quantityChange: Int,
        referenceId: Long
    ) {
        inventoryHistoryRepository.save(
            InventoryHistory.create(inventoryId, historyType, quantityChange, referenceId)
        )
    }

    companion object {
        private const val RECENT_HISTORY_LIMIT = 5
    }
}
