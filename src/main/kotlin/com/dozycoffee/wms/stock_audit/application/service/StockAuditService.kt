package com.dozycoffee.wms.stock_audit.application.service

import com.dozycoffee.wms.global.security.CurrentActorProvider
import com.dozycoffee.wms.global.security.UserActor
import com.dozycoffee.wms.global.security.WarehouseAccessGuard
import com.dozycoffee.wms.global.security.WmsRole
import com.dozycoffee.wms.location_stock.application.port.`in`.DecreaseLocationStockUseCase
import com.dozycoffee.wms.location_stock.application.port.`in`.GetLocationStockUseCase
import com.dozycoffee.wms.location_stock.application.port.`in`.IncreaseLocationStockUseCase
import com.dozycoffee.wms.location_stock.application.port.`in`.command.DecreaseLocationStockCommand
import com.dozycoffee.wms.location_stock.application.port.`in`.command.IncreaseLocationStockCommand
import com.dozycoffee.wms.location_stock.application.port.`in`.result.LocationStockResult
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
import com.dozycoffee.wms.stock_audit.domain.exception.StockAuditZoneWarehouseMismatchException
import com.dozycoffee.wms.stock_audit.domain.model.StockAudit
import com.dozycoffee.wms.stock_audit.domain.model.StockAuditItem
import com.dozycoffee.wms.warehouse.application.port.`in`.GetLocationUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.GetZoneUseCase
import kotlin.math.abs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class StockAuditService(
    private val warehouseAccessGuard: WarehouseAccessGuard,
    private val stockAuditRepository: StockAuditRepository,
    private val stockAuditItemRepository: StockAuditItemRepository,
    private val getZoneUseCase: GetZoneUseCase,
    private val getLocationUseCase: GetLocationUseCase,
    private val getLocationStockUseCase: GetLocationStockUseCase,
    private val increaseLocationStockUseCase: IncreaseLocationStockUseCase,
    private val decreaseLocationStockUseCase: DecreaseLocationStockUseCase,
    private val currentActorProvider: CurrentActorProvider,
    private val stockAuditInventoryAdjuster: StockAuditInventoryAdjuster,
    @param:Value("\${wms.stock-audit.adjustment-approval-threshold}")
    private val adjustmentApprovalThreshold: Int
) : RegisterStockAuditUseCase,
    GetStockAuditUseCase,
    AssignStockAuditUseCase,
    CompleteStockAuditUseCase,
    CloseStockAuditUseCase {

    /** 실사 등록 — 대상 Zone의 모든 Location 장부 행(수량이 있는 것)을 스냅샷으로 남긴다(품질상태 무관, 물리적 실재 수량 기준) */
    @Transactional
    override suspend fun register(command: RegisterStockAuditCommand): StockAuditResult {
        warehouseAccessGuard.require(command.warehouseId)
        val zone = getZoneUseCase.getById(command.zoneId)
        if (zone.warehouseId != command.warehouseId) throw StockAuditZoneWarehouseMismatchException()
        val stockAudit = StockAudit.create(command.warehouseId, command.zoneId)
        val saved = stockAuditRepository.save(stockAudit)
        val stockAuditId: Long = requireNotNull(saved.stockAuditId)

        val locations = getLocationUseCase.getByZoneId(command.zoneId).toList()
        for (location in locations) {
            for (stock in getLocationStockUseCase.getAllByLocationId(location.locationId).filter { it.quantity > 0 }) {
                stockAuditItemRepository.save(StockAuditItem.create(stockAuditId, stock.locationStockId, stock.quantity))
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
    override suspend fun assign(stockAuditId: Long): StockAuditResult {
        val stockAudit = findAccessibleStockAuditOrThrow(stockAuditId)
        stockAudit.assign((currentActorProvider.get() as? UserActor)?.auditName)
        return StockAuditResult.from(stockAuditRepository.save(stockAudit))
    }

    /** 실사 완료 — 전 항목 카운트 여부를 확인하고, 스냅샷 이후 장부 행이 바뀐 항목을 표시한다 */
    @Transactional
    override suspend fun complete(stockAuditId: Long): StockAuditResult {
        val stockAudit = findAccessibleStockAuditOrThrow(stockAuditId)
        val items = stockAuditItemRepository.findAllByStockAuditId(stockAuditId).toList()

        if (items.any { !it.isCounted }) {
            throw StockAuditItemsNotFullyCountedException()
        }

        for (item in items) {
            if (getLocationStockUseCase.hasChangedSince(item.locationStockId, item.snapshotTakenAt)) {
                item.markHasUncommittedMovement()
                stockAuditItemRepository.save(item)
            }
        }

        stockAudit.complete()
        return StockAuditResult.from(stockAuditRepository.save(stockAudit))
    }

    /**
     * 조정 확정 — 항목별 조정량(실측 - 마감 시점 실시간 Inventory 수량, ADR-0009)을 계산해 임계치 초과
     * 여부를 판단한 뒤, 승인 조건을 만족해야만 조정을 실제로 반영한다. 조정량만큼 위치 재고와 Location 사용량이 함께 증감한다.
     * 로컬 반영을 마친 뒤 마지막에 inventory에 조정을 동기로 반영하고, inventory가 거부하거나 응답하지 않으면 예외로 로컬 반영도 되돌린다.
     * 같은 멱등 키(`wms-stock-audit-{실사 ID}`)로 다시 마감해도 inventory에 중복 반영되지 않는다.
     */
    @Transactional
    override suspend fun close(stockAuditId: Long): StockAuditResult {
        val stockAudit = findAccessibleStockAuditOrThrow(stockAuditId)
        val items = stockAuditItemRepository.findAllByStockAuditId(stockAuditId).toList()

        val adjustments = items.mapNotNull { item ->
            val countedQuantity = requireNotNull(item.countedQuantity) { "완료된 실사의 항목은 카운트가 있어야 합니다." }
            val currentStock = getLocationStockUseCase.getById(item.locationStockId)
            val adjustmentAmount = countedQuantity - currentStock.quantity
            if (adjustmentAmount == 0) null else Adjustment(item, adjustmentAmount, currentStock)
        }
        val inventoryLines = toInventoryLines(adjustments)
        if (inventoryLines.isNotEmpty()) {
            stockAuditInventoryAdjuster.requireNoBacklog(stockAudit.warehouseId)
        }

        val requiresApproval = adjustments.any { abs(it.amount) > adjustmentApprovalThreshold } ||
            inventoryLines.any { abs(it.quantityChange) > adjustmentApprovalThreshold }
        val approver: String? = resolveApprover()
        stockAudit.close(requiresApproval, if (requiresApproval) approver else null)

        adjustments.forEach { applyToLocationStock(it) }

        if (inventoryLines.isNotEmpty()) {
            stockAuditInventoryAdjuster.adjust(stockAudit.warehouseId, stockAuditId, approver, inventoryLines)
        }

        return StockAuditResult.from(stockAuditRepository.save(stockAudit))
    }

    /** Location 항목의 조정량을 inventory의 조정 단위(상품 x Lot 번호 x 품질 상태)로 합산한다. 합이 0인 단위는 보내지 않는다 */
    private fun toInventoryLines(adjustments: List<Adjustment>): List<StockAuditInventoryAdjuster.Line> {
        val amountByKey = linkedMapOf<Triple<Long, String, String>, Int>()
        for (adjustment in adjustments) {
            val stock = adjustment.stock
            val key = Triple(stock.productId, stock.lotNumber, stock.qualityStatus.name)
            amountByKey.merge(key, adjustment.amount, Int::plus)
        }
        return amountByKey
            .filterValues { it != 0 }
            .map { (key, amount) -> StockAuditInventoryAdjuster.Line(key.first, key.second, key.third, amount) }
    }

    /** 임계치 초과 조정은 상위 관리자([WmsRole.WAREHOUSE_ADMIN])만 승인할 수 있다. 자격이 없으면 승인자 없음으로 취급한다 */
    private suspend fun resolveApprover(): String? {
        val actor = currentActorProvider.get()
        return (actor as? UserActor)?.takeIf { WmsRole.WAREHOUSE_ADMIN.code in it.roles }?.auditName
    }

    private suspend fun applyToLocationStock(adjustment: Adjustment) {
        val stock = adjustment.stock
        if (adjustment.amount > 0) {
            increaseLocationStockUseCase.increase(
                IncreaseLocationStockCommand(stock.locationId, stock.productId, stock.lotNumber, stock.qualityStatus, adjustment.amount)
            )
        } else {
            decreaseLocationStockUseCase.decrease(
                DecreaseLocationStockCommand(stock.locationId, stock.productId, stock.lotNumber, stock.qualityStatus, -adjustment.amount)
            )
        }
    }

    private suspend fun findAccessibleStockAuditOrThrow(stockAuditId: Long): StockAudit {
        val stockAudit = stockAuditRepository.findById(stockAuditId) ?: throw StockAuditNotFoundException()
        warehouseAccessGuard.require(stockAudit.warehouseId)
        return stockAudit
    }

    private data class Adjustment(val item: StockAuditItem, val amount: Int, val stock: LocationStockResult)
}
