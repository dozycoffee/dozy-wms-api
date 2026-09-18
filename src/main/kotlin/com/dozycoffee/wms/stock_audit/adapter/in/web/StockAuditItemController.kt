package com.dozycoffee.wms.stock_audit.adapter.`in`.web

import com.dozycoffee.wms.stock_audit.adapter.`in`.web.request.CountStockAuditItemRequest
import com.dozycoffee.wms.stock_audit.adapter.`in`.web.response.StockAuditItemResponse
import com.dozycoffee.wms.stock_audit.application.port.`in`.CountStockAuditItemUseCase
import com.dozycoffee.wms.stock_audit.application.port.`in`.GetStockAuditItemUseCase
import com.dozycoffee.wms.stock_audit.application.port.`in`.command.CountStockAuditItemCommand
import jakarta.validation.Valid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/stock-audit-items")
class StockAuditItemController(
    private val getStockAuditItemUseCase: GetStockAuditItemUseCase,
    private val countStockAuditItemUseCase: CountStockAuditItemUseCase
) {

    @GetMapping
    fun getAllByStockAudit(@RequestParam stockAuditId: Long): Flow<StockAuditItemResponse> {
        return getStockAuditItemUseCase.getAllByStockAudit(stockAuditId).map { StockAuditItemResponse.from(it) }
    }

    @PatchMapping("/{stockAuditItemId}/count")
    suspend fun count(
        @PathVariable stockAuditItemId: Long,
        @Valid @RequestBody request: CountStockAuditItemRequest
    ): StockAuditItemResponse {
        val command = CountStockAuditItemCommand(stockAuditItemId, requireNotNull(request.countedQuantity))
        return StockAuditItemResponse.from(countStockAuditItemUseCase.count(command))
    }
}
