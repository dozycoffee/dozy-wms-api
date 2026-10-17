package com.dozycoffee.wms.location_stock.adapter.`in`.web

import com.dozycoffee.wms.global.security.WmsAuthorize
import com.dozycoffee.wms.location_stock.adapter.`in`.web.response.LocationStockListResponse
import com.dozycoffee.wms.location_stock.application.port.`in`.ListLocationStocksUseCase
import com.dozycoffee.wms.location_stock.application.port.`in`.query.LocationStockQuery
import com.dozycoffee.wms.location_stock.domain.enumeration.StockQualityStatus
import kotlinx.coroutines.flow.toList
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** Location별 재고(장부 행) 조회. 상품명 등 inventory·상품 마스터 정보는 주지 않는다(ADR-0027) */
@RestController
@RequestMapping("/api/location-stocks")
class LocationStockQueryController(
    private val listLocationStocksUseCase: ListLocationStocksUseCase
) {

    @GetMapping
    @PreAuthorize(WmsAuthorize.READ)
    suspend fun getAll(
        @RequestParam(required = false) warehouseIds: List<Long>?,
        @RequestParam(required = false) zoneId: Long?,
        @RequestParam(required = false) locationId: Long?,
        @RequestParam(required = false) productId: Long?,
        @RequestParam(required = false) qualityStatus: StockQualityStatus?,
        @RequestParam(defaultValue = "false") includeEmpty: Boolean
    ): LocationStockListResponse {
        val query = LocationStockQuery(warehouseIds, zoneId, locationId, productId, qualityStatus, includeEmpty)
        return LocationStockListResponse.from(listLocationStocksUseCase.getAll(query).toList())
    }
}
