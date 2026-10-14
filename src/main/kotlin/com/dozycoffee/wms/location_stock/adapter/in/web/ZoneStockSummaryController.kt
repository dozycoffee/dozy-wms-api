package com.dozycoffee.wms.location_stock.adapter.`in`.web

import com.dozycoffee.wms.global.security.WmsAuthorize
import com.dozycoffee.wms.location_stock.adapter.`in`.web.response.ZoneStockSummaryResponse
import com.dozycoffee.wms.location_stock.application.port.`in`.GetZoneStockSummaryUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** Zone 단위 재고 현황. 수량·Lot·이력 조회는 inventory 서비스 API가 맡고 WMS는 Location 장부로 만들 수 있는 현황만 제공한다(ADR-0018 결정 6) */
@RestController
@RequestMapping("/api/inventories")
class ZoneStockSummaryController(
    private val getZoneStockSummaryUseCase: GetZoneStockSummaryUseCase
) {

    @GetMapping("/zone-summary")
    @PreAuthorize(WmsAuthorize.READ)
    fun getZoneSummary(
        @RequestParam(required = false) warehouseIds: List<Long>?
    ): Flow<ZoneStockSummaryResponse> {
        return getZoneStockSummaryUseCase.getAll(warehouseIds).map { ZoneStockSummaryResponse.from(it) }
    }
}
