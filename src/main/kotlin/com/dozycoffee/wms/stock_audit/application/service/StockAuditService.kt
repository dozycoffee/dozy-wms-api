package com.dozycoffee.wms.stock_audit.application.service

import com.dozycoffee.wms.global.security.CurrentActorProvider
import com.dozycoffee.wms.global.security.UserActor
import com.dozycoffee.wms.global.security.WarehouseAccessGuard
import com.dozycoffee.wms.global.security.WmsRole
import com.dozycoffee.wms.inventory.application.port.`in`.AdjustInventoryQuantityUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.GetInventoryUseCase
import com.dozycoffee.wms.inventory.application.port.out.InventoryHistoryRepository
import com.dozycoffee.wms.stock_audit.application.port.`in`.AssignStockAuditUseCase
import com.dozycoffee.wms.stock_audit.application.port.`in`.CloseStockAuditUseCase
import com.dozycoffee.wms.stock_audit.application.port.`in`.CompleteStockAuditUseCase
import com.dozycoffee.wms.stock_audit.application.port.`in`.GetStockAuditUseCase
import com.dozycoffee.wms.stock_audit.application.port.`in`.RegisterStockAuditUseCase
import com.dozycoffee.wms.stock_audit.application.port.`in`.command.RegisterStockAuditCommand
import com.dozycoffee.wms.stock_audit.application.port.`in`.result.StockAuditResult
import com.dozycoffee.wms.stock_audit.application.port.out.StockAuditItemRepository
import com.dozycoffee.wms.stock_audit.application.port.out.StockAuditRepository
import com.dozycoffee.wms.stock_audit.domain.enumeration.StockAuditStatus
import com.dozycoffee.wms.stock_audit.domain.exception.StockAuditItemsNotFullyCountedException
import com.dozycoffee.wms.stock_audit.domain.exception.StockAuditNotFoundException
import com.dozycoffee.wms.stock_audit.domain.model.StockAudit
import com.dozycoffee.wms.stock_audit.domain.model.StockAuditItem
import com.dozycoffee.wms.warehouse.application.port.`in`.GetLocationUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.OccupyLocationUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.ReleaseLocationUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.command.OccupyLocationCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.command.ReleaseLocationCommand
import kotlin.math.abs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class StockAuditService(
    private val warehouseAccessGuard: WarehouseAccessGuard,
    private val stockAuditRepository: StockAuditRepository,
    private val stockAuditItemRepository: StockAuditItemRepository,
    private val getLocationUseCase: GetLocationUseCase,
    private val getInventoryUseCase: GetInventoryUseCase,
    private val adjustInventoryQuantityUseCase: AdjustInventoryQuantityUseCase,
    private val occupyLocationUseCase: OccupyLocationUseCase,
    private val releaseLocationUseCase: ReleaseLocationUseCase,
    private val inventoryHistoryRepository: InventoryHistoryRepository,
    private val currentActorProvider: CurrentActorProvider,
    @param:Value("\${wms.stock-audit.adjustment-approval-threshold}")
    private val adjustmentApprovalThreshold: Int
) : RegisterStockAuditUseCase,
    GetStockAuditUseCase,
    AssignStockAuditUseCase,
    CompleteStockAuditUseCase,
    CloseStockAuditUseCase {

    /** 실사 등록 — 대상 Zone의 모든 Location에 속한 Inventory를 스냅샷으로 남긴다(품질상태 무관, 물리적 실재 수량 기준) */
    @Transactional
    override suspend fun register(command: RegisterStockAuditCommand): StockAuditResult {
        warehouseAccessGuard.require(command.warehouseId)
        val stockAudit = StockAudit.create(command.warehouseId, command.zoneId)
        val saved = stockAuditRepository.save(stockAudit)
        val stockAuditId: Long = requireNotNull(saved.stockAuditId)

        val locations = getLocationUseCase.getByZoneId(command.zoneId).collectList().awaitSingle()
        for (location in locations) {
            val inventories = getInventoryUseCase.getAll(location.locationId, null, null, null, null).toList()
            for (inventory in inventories) {
                stockAuditItemRepository.save(
                    StockAuditItem.create(stockAuditId, inventory.inventoryId, inventory.quantity)
                )
            }
        }

        return StockAuditResult.from(saved)
    }

    @Transactional(readOnly = true)
    override suspend fun getById(stockAuditId: Long): StockAuditResult {
        return StockAuditResult.from(findAccessibleStockAuditOrThrow(stockAuditId))
    }

    @Transactional(readOnly = true)
    override fun getAll(warehouseId: Long?, status: StockAuditStatus?): Flow<StockAuditResult> {
        return warehouseAccessGuard.scoped(warehouseId?.let { listOf(it) }) { warehouseIds ->
            stockAuditRepository.findAll(warehouseIds, status)
        }.map { StockAuditResult.from(it) }
    }

    @Transactional
    override suspend fun assign(stockAuditId: Long, assignee: String): StockAuditResult {
        val stockAudit = findAccessibleStockAuditOrThrow(stockAuditId)
        stockAudit.assign(assignee)
        return StockAuditResult.from(stockAuditRepository.save(stockAudit))
    }

    /** 실사 완료 — 전 항목 카운트 여부를 확인하고, 스냅샷 이후 미반영 입출고 이력이 있는 항목을 표시한다 */
    @Transactional
    override suspend fun complete(stockAuditId: Long): StockAuditResult {
        val stockAudit = findAccessibleStockAuditOrThrow(stockAuditId)
        val items = stockAuditItemRepository.findAllByStockAuditId(stockAuditId).toList()

        if (items.any { !it.isCounted }) {
            throw StockAuditItemsNotFullyCountedException()
        }

        for (item in items) {
            val hasMovementSinceSnapshot =
                inventoryHistoryRepository.findAll(item.inventoryId, null, item.snapshotTakenAt, null)
                    .toList()
                    .isNotEmpty()
            if (hasMovementSinceSnapshot) {
                item.markHasUncommittedMovement()
                stockAuditItemRepository.save(item)
            }
        }

        stockAudit.complete()
        return StockAuditResult.from(stockAuditRepository.save(stockAudit))
    }

    /**
     * 조정 확정 — 항목별 조정량(실측 - 마감 시점 실시간 Inventory 수량, ADR-0009)을 계산해 임계치 초과
     * 여부를 판단한 뒤, 승인 조건을 만족해야만 조정을 실제로 반영한다. 조정량만큼 Location 사용량도 함께 증감한다
     */
    @Transactional
    override suspend fun close(stockAuditId: Long): StockAuditResult {
        val stockAudit = findAccessibleStockAuditOrThrow(stockAuditId)
        val items = stockAuditItemRepository.findAllByStockAuditId(stockAuditId).toList()

        val adjustments = items.mapNotNull { item ->
            val countedQuantity = requireNotNull(item.countedQuantity) { "완료된 실사의 항목은 카운트가 있어야 합니다." }
            val currentInventory = getInventoryUseCase.getById(item.inventoryId)
            val adjustmentAmount = countedQuantity - currentInventory.quantity
            if (adjustmentAmount == 0) null else Adjustment(item, adjustmentAmount, currentInventory.locationId)
        }

        val requiresApproval = adjustments.any { abs(it.amount) > adjustmentApprovalThreshold }
        stockAudit.close(requiresApproval, if (requiresApproval) resolveApprover() else null)

        for (adjustment in adjustments) {
            adjustInventoryQuantityUseCase.adjust(
                adjustment.item.inventoryId,
                requireNotNull(adjustment.item.countedQuantity),
                requireNotNull(adjustment.item.stockAuditItemId)
            )
            syncLocationUsage(adjustment)
        }

        return StockAuditResult.from(stockAuditRepository.save(stockAudit))
    }

    /** 임계치 초과 조정은 상위 관리자([WmsRole.WAREHOUSE_ADMIN])만 승인할 수 있다. 자격이 없으면 승인자 없음으로 취급한다 */
    private suspend fun resolveApprover(): String? {
        val actor = currentActorProvider.get()
        return (actor as? UserActor)?.takeIf { WmsRole.WAREHOUSE_ADMIN.code in it.roles }?.auditName
    }

    private suspend fun syncLocationUsage(adjustment: Adjustment) {
        if (adjustment.amount > 0) {
            occupyLocationUseCase.occupy(OccupyLocationCommand(adjustment.locationId, adjustment.amount)).awaitSingle()
        } else {
            releaseLocationUseCase.release(ReleaseLocationCommand(adjustment.locationId, -adjustment.amount)).awaitSingle()
        }
    }

    private suspend fun findAccessibleStockAuditOrThrow(stockAuditId: Long): StockAudit {
        val stockAudit = stockAuditRepository.findById(stockAuditId) ?: throw StockAuditNotFoundException()
        warehouseAccessGuard.require(stockAudit.warehouseId)
        return stockAudit
    }

    private data class Adjustment(val item: StockAuditItem, val amount: Int, val locationId: Long)
}
