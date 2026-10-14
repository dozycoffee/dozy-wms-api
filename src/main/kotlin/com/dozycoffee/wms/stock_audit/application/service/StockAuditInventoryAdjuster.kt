package com.dozycoffee.wms.stock_audit.application.service

import com.dozycoffee.wms.inventory_call.application.port.`in`.CallInventoryDirectlyUseCase
import com.dozycoffee.wms.inventory_call.application.port.`in`.CheckInventoryCallBacklogUseCase
import com.dozycoffee.wms.inventory_call.application.port.`in`.result.InventoryDirectCallResult
import com.dozycoffee.wms.stock_audit.domain.exception.StockAuditInventoryCallsPendingException
import com.dozycoffee.wms.stock_audit.domain.exception.StockAuditInventoryRejectedException
import com.dozycoffee.wms.stock_audit.domain.exception.StockAuditInventoryUnavailableException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

/**
 * 실사 조정을 inventory에 동기로 반영한다 (ADR-0023). inventory는 가용 수량 초과·승인 누락 같은 업무 거부를 하므로
 * 아웃박스가 아니라 직접 호출해 마감 요청에서 바로 알린다. 같은 창고에 먼저 보낼 호출이 남아 있으면 순서가 꼬이므로 거부한다.
 */
@Component
class StockAuditInventoryAdjuster(
    private val callInventoryDirectlyUseCase: CallInventoryDirectlyUseCase,
    private val checkInventoryCallBacklogUseCase: CheckInventoryCallBacklogUseCase,
    private val jsonMapper: JsonMapper
) {

    suspend fun requireNoBacklog(warehouseId: Long) {
        if (checkInventoryCallBacklogUseCase.hasUnfinished(warehouseId)) {
            throw StockAuditInventoryCallsPendingException()
        }
    }

    suspend fun adjust(warehouseId: Long, stockAuditId: Long, approvedBy: String?, lines: List<Line>) {
        val body = AdjustmentRequestBody(
            warehouseId = warehouseId,
            auditId = stockAuditId,
            approvedBy = approvedBy,
            items = lines.map { AdjustmentItemBody(it.productId, it.lotNumber, it.qualityStatus, it.quantityChange) }
        )
        val result = callInventoryDirectlyUseCase.post(
            REQUEST_PATH,
            "wms-stock-audit-$stockAuditId",
            jsonMapper.writeValueAsString(body)
        )
        when (result) {
            is InventoryDirectCallResult.Accepted -> Unit
            is InventoryDirectCallResult.Rejected -> {
                log.warn("inventory 실사 조정 거부: stockAuditId={}, status={}, body={}", stockAuditId, result.status, result.responseBody)
                throw StockAuditInventoryRejectedException()
            }
            is InventoryDirectCallResult.Unavailable -> {
                log.warn("inventory 실사 조정 호출 불가: stockAuditId={}, reason={}", stockAuditId, result.reason)
                throw StockAuditInventoryUnavailableException()
            }
        }
    }

    data class Line(val productId: Long, val lotNumber: String, val qualityStatus: String, val quantityChange: Int)

    private data class AdjustmentRequestBody(
        val warehouseId: Long,
        val auditId: Long,
        val approvedBy: String?,
        val items: List<AdjustmentItemBody>
    )

    private data class AdjustmentItemBody(
        val productId: Long,
        val lotNumber: String,
        val qualityStatus: String,
        val quantityChange: Int
    )

    private companion object {
        const val REQUEST_PATH: String = "/api/v1/stock-adjustments"
        private val log = LoggerFactory.getLogger(StockAuditInventoryAdjuster::class.java)
    }
}
