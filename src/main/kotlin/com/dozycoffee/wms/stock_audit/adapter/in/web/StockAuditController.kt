package com.dozycoffee.wms.stock_audit.adapter.`in`.web

import com.dozycoffee.wms.global.security.WmsAuthorize
import com.dozycoffee.wms.stock_audit.adapter.`in`.web.request.RegisterStockAuditRequest
import com.dozycoffee.wms.stock_audit.adapter.`in`.web.response.StockAuditResponse
import com.dozycoffee.wms.stock_audit.application.port.`in`.AssignStockAuditUseCase
import com.dozycoffee.wms.stock_audit.application.port.`in`.CloseStockAuditUseCase
import com.dozycoffee.wms.stock_audit.application.port.`in`.CompleteStockAuditUseCase
import com.dozycoffee.wms.stock_audit.application.port.`in`.GetStockAuditUseCase
import com.dozycoffee.wms.stock_audit.application.port.`in`.RegisterStockAuditUseCase
import com.dozycoffee.wms.stock_audit.domain.enumeration.StockAuditStatus
import jakarta.validation.Valid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/stock-audits")
class StockAuditController(
    private val registerStockAuditUseCase: RegisterStockAuditUseCase,
    private val getStockAuditUseCase: GetStockAuditUseCase,
    private val assignStockAuditUseCase: AssignStockAuditUseCase,
    private val completeStockAuditUseCase: CompleteStockAuditUseCase,
    private val closeStockAuditUseCase: CloseStockAuditUseCase
) {

    @PostMapping
    @PreAuthorize(WmsAuthorize.STOCK_AUDIT)
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun register(@Valid @RequestBody request: RegisterStockAuditRequest): StockAuditResponse {
        return StockAuditResponse.from(registerStockAuditUseCase.register(request.toCommand()))
    }

    @GetMapping("/{stockAuditId}")
    @PreAuthorize(WmsAuthorize.READ)
    suspend fun getById(@PathVariable stockAuditId: Long): StockAuditResponse {
        return StockAuditResponse.from(getStockAuditUseCase.getById(stockAuditId))
    }

    @GetMapping
    @PreAuthorize(WmsAuthorize.READ)
    fun getAll(
        @RequestParam(required = false) warehouseId: Long?,
        @RequestParam(required = false) status: StockAuditStatus?
    ): Flow<StockAuditResponse> {
        return getStockAuditUseCase.getAll(warehouseId, status).map { StockAuditResponse.from(it) }
    }

    @PatchMapping("/{stockAuditId}/assign")
    @PreAuthorize(WmsAuthorize.STOCK_AUDIT)
    suspend fun assign(
        @PathVariable stockAuditId: Long
    ): StockAuditResponse {
        return StockAuditResponse.from(assignStockAuditUseCase.assign(stockAuditId))
    }

    @PatchMapping("/{stockAuditId}/complete")
    @PreAuthorize(WmsAuthorize.STOCK_AUDIT)
    suspend fun complete(@PathVariable stockAuditId: Long): StockAuditResponse {
        return StockAuditResponse.from(completeStockAuditUseCase.complete(stockAuditId))
    }

    @PatchMapping("/{stockAuditId}/close")
    @PreAuthorize(WmsAuthorize.STOCK_AUDIT)
    suspend fun close(@PathVariable stockAuditId: Long): StockAuditResponse {
        return StockAuditResponse.from(closeStockAuditUseCase.close(stockAuditId))
    }
}
