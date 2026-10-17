package com.dozycoffee.wms.location_stock.application.service

import com.dozycoffee.wms.global.security.CurrentWarehouseAccessProvider
import com.dozycoffee.wms.global.security.WarehouseFilter
import com.dozycoffee.wms.location_stock.application.port.`in`.ListLocationStocksUseCase
import com.dozycoffee.wms.location_stock.application.port.`in`.query.LocationStockQuery
import com.dozycoffee.wms.location_stock.application.port.`in`.result.LocationStockDetailResult
import com.dozycoffee.wms.location_stock.application.port.out.LocationStockQueryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LocationStockQueryService(
    private val locationStockQueryRepository: LocationStockQueryRepository,
    private val currentWarehouseAccessProvider: CurrentWarehouseAccessProvider
) : ListLocationStocksUseCase {

    @Transactional(readOnly = true)
    override fun getAll(query: LocationStockQuery): Flow<LocationStockDetailResult> = flow {
        when (val filter = currentWarehouseAccessProvider.current().narrow(query.warehouseIds)) {
            WarehouseFilter.None -> return@flow
            WarehouseFilter.Unfiltered -> emitAll(locationStockQueryRepository.findAll(query.copy(warehouseIds = null)))
            is WarehouseFilter.In -> emitAll(locationStockQueryRepository.findAll(query.copy(warehouseIds = filter.warehouseIds)))
        }
    }
}
