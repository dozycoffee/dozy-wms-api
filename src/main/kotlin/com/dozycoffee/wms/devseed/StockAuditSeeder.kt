package com.dozycoffee.wms.devseed

import com.dozycoffee.wms.inventory.application.port.`in`.GetInventoryUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.CompleteOutboundUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.RegisterOutboundUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.StartOutboundInspectingUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.StartOutboundPickingUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.command.RegisterOutboundCommand
import com.dozycoffee.wms.outbound.application.port.`in`.command.RegisterOutboundItemCommand
import com.dozycoffee.wms.stock_audit.application.port.`in`.AssignStockAuditUseCase
import com.dozycoffee.wms.stock_audit.application.port.`in`.CloseStockAuditUseCase
import com.dozycoffee.wms.stock_audit.application.port.`in`.CompleteStockAuditUseCase
import com.dozycoffee.wms.stock_audit.application.port.`in`.CountStockAuditItemUseCase
import com.dozycoffee.wms.stock_audit.application.port.`in`.GetStockAuditItemUseCase
import com.dozycoffee.wms.stock_audit.application.port.`in`.RegisterStockAuditUseCase
import com.dozycoffee.wms.stock_audit.application.port.`in`.command.CountStockAuditItemCommand
import com.dozycoffee.wms.stock_audit.application.port.`in`.command.RegisterStockAuditCommand
import com.dozycoffee.wms.stock_audit.application.port.`in`.result.StockAuditItemResult
import com.dozycoffee.wms.stock_audit.application.port.`in`.result.StockAuditResult
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Component

/** 실사 4단계(SCHEDULED/IN_PROGRESS/COMPLETED/CLOSED)를 Zone별로 하나씩 만든다 */
@Component
internal class StockAuditSeeder(
    private val registerStockAuditUseCase: RegisterStockAuditUseCase,
    private val assignStockAuditUseCase: AssignStockAuditUseCase,
    private val countStockAuditItemUseCase: CountStockAuditItemUseCase,
    private val getStockAuditItemUseCase: GetStockAuditItemUseCase,
    private val completeStockAuditUseCase: CompleteStockAuditUseCase,
    private val closeStockAuditUseCase: CloseStockAuditUseCase,
    private val getInventoryUseCase: GetInventoryUseCase,
    private val registerOutboundUseCase: RegisterOutboundUseCase,
    private val startOutboundPickingUseCase: StartOutboundPickingUseCase,
    private val startOutboundInspectingUseCase: StartOutboundInspectingUseCase,
    private val completeOutboundUseCase: CompleteOutboundUseCase
) {

    suspend fun seed(context: SeedContext, baselineInventoryIds: Map<String, Long>) {
        register(context, ZoneCode.F)
        seedInProgress(context)
        seedCompletedWithMovement(context, baselineInventoryIds)
        seedClosedWithAdjustment(context, baselineInventoryIds)
    }

    private suspend fun seedInProgress(context: SeedContext) {
        val audit: StockAuditResult = register(context, ZoneCode.B)
        assignStockAuditUseCase.assign(audit.stockAuditId, "김창고")
        val first: StockAuditItemResult = items(audit).first()
        countStockAuditItemUseCase.count(CountStockAuditItemCommand(first.stockAuditItemId, first.snapshotQuantity))
    }

    /** 스냅샷 이후 출고가 발생한 재고가 `hasUncommittedMovement`로 표시되는 사례 */
    private suspend fun seedCompletedWithMovement(context: SeedContext, baselineInventoryIds: Map<String, Long>) {
        val audit: StockAuditResult = register(context, ZoneCode.A)
        assignStockAuditUseCase.assign(audit.stockAuditId, "이재고")

        val outbound = registerOutboundUseCase.register(
            RegisterOutboundCommand(
                context.warehouseId,
                listOf(RegisterOutboundItemCommand(context.productIdByCode.getValue("BEAN-002"), MOVEMENT_QUANTITY))
            )
        )
        startOutboundPickingUseCase.startPicking(outbound.outboundId)
        startOutboundInspectingUseCase.startInspecting(outbound.outboundId)
        completeOutboundUseCase.complete(outbound.outboundId)

        countAll(audit, mapOf(baselineInventoryIds.getValue("BEAN003-L1") to MISCOUNT_SHORTAGE))
        completeStockAuditUseCase.complete(audit.stockAuditId)
    }

    /** 임계치를 초과하는 과잉 차이(+15)와 소량 부족(-3)을 승인자와 함께 확정해 ADJUSTMENT 이력을 남긴다 */
    private suspend fun seedClosedWithAdjustment(context: SeedContext, baselineInventoryIds: Map<String, Long>) {
        val audit: StockAuditResult = register(context, ZoneCode.C)
        assignStockAuditUseCase.assign(audit.stockAuditId, "박재고")
        countAll(
            audit,
            mapOf(
                baselineInventoryIds.getValue("PWD003-L1") to SURPLUS_OVER_THRESHOLD,
                baselineInventoryIds.getValue("PWD001-L1") to -SMALL_SHORTAGE
            )
        )
        completeStockAuditUseCase.complete(audit.stockAuditId)
        closeStockAuditUseCase.close(audit.stockAuditId, "관리팀장")
    }

    private suspend fun register(context: SeedContext, zoneCode: ZoneCode): StockAuditResult =
        registerStockAuditUseCase.register(
            RegisterStockAuditCommand(context.warehouseId, context.zoneIdByCode.getValue(zoneCode))
        )

    private suspend fun items(audit: StockAuditResult): List<StockAuditItemResult> =
        getStockAuditItemUseCase.getAllByStockAudit(audit.stockAuditId).toList()

    /** 모든 항목을 현재 시스템 수량으로 입력하되, 지정한 재고만 차이(실측 - 현재 수량)를 적용한다 */
    private suspend fun countAll(audit: StockAuditResult, differenceByInventoryId: Map<Long, Int>) {
        items(audit).forEach { item ->
            val currentQuantity: Int = getInventoryUseCase.getById(item.inventoryId).quantity
            val difference: Int = differenceByInventoryId[item.inventoryId] ?: 0
            countStockAuditItemUseCase.count(CountStockAuditItemCommand(item.stockAuditItemId, currentQuantity + difference))
        }
    }

    private companion object {
        const val MOVEMENT_QUANTITY: Int = 10
        const val MISCOUNT_SHORTAGE: Int = -2
        const val SURPLUS_OVER_THRESHOLD: Int = 15
        const val SMALL_SHORTAGE: Int = 3
    }
}
