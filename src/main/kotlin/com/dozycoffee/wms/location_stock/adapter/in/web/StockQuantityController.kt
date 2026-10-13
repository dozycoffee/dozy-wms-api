package com.dozycoffee.wms.location_stock.adapter.`in`.web

import com.dozycoffee.wms.global.security.WmsAuthorize
import com.dozycoffee.wms.location_stock.adapter.`in`.web.response.StockQuantitiesResponse
import com.dozycoffee.wms.location_stock.application.port.`in`.GetWarehouseStockQuantitiesUseCase
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** inventory 서비스의 대사 배치와 보정안 승인이 창고의 실제 수량을 조회하는 API다(ADR-0017, inventory ADR-0033) */
@RestController
@RequestMapping("/api/v1/stock-quantities")
class StockQuantityController(
    private val getWarehouseStockQuantitiesUseCase: GetWarehouseStockQuantitiesUseCase
) {

    @GetMapping
    @PreAuthorize(WmsAuthorize.SERVICE)
    suspend fun getQuantities(
        @RequestParam warehouseId: Long,
        @RequestParam(required = false) productId: Long?,
        @RequestParam(required = false) lotNumber: String?
    ): StockQuantitiesResponse {
        return StockQuantitiesResponse.from(
            getWarehouseStockQuantitiesUseCase.getQuantities(warehouseId, productId, lotNumber)
        )
    }
}
