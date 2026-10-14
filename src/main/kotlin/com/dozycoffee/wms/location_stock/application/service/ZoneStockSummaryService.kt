package com.dozycoffee.wms.location_stock.application.service

import com.dozycoffee.wms.global.security.CurrentWarehouseAccessProvider
import com.dozycoffee.wms.global.security.WarehouseFilter
import com.dozycoffee.wms.location_stock.application.port.`in`.GetZoneStockSummaryUseCase
import com.dozycoffee.wms.location_stock.application.port.`in`.result.ZoneStockSummaryResult
import com.dozycoffee.wms.location_stock.application.port.out.ZoneStockSummaryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ZoneStockSummaryService(
    private val zoneStockSummaryRepository: ZoneStockSummaryRepository,
    private val currentWarehouseAccessProvider: CurrentWarehouseAccessProvider
) : GetZoneStockSummaryUseCase {

    @Transactional(readOnly = true)
    override fun getAll(warehouseIds: List<Long>?): Flow<ZoneStockSummaryResult> = flow {
        when (val filter = currentWarehouseAccessProvider.current().narrow(warehouseIds)) {
            WarehouseFilter.None -> return@flow
            WarehouseFilter.Unfiltered -> emitAll(zoneStockSummaryRepository.findAll(null))
            is WarehouseFilter.In -> emitAll(zoneStockSummaryRepository.findAll(filter.warehouseIds))
        }
    }
}
