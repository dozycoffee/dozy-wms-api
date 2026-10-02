package com.dozycoffee.wms.inventory.application.service

import com.dozycoffee.wms.global.security.CurrentWarehouseAccessProvider
import com.dozycoffee.wms.global.security.WarehouseFilter
import com.dozycoffee.wms.inventory.application.port.`in`.GetZoneInventorySummaryUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.result.ZoneInventorySummaryResult
import com.dozycoffee.wms.inventory.application.port.out.ZoneInventorySummaryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ZoneInventorySummaryService(
    private val zoneInventorySummaryRepository: ZoneInventorySummaryRepository,
    private val currentWarehouseAccessProvider: CurrentWarehouseAccessProvider
) : GetZoneInventorySummaryUseCase {

    @Transactional(readOnly = true)
    override fun getAll(warehouseIds: List<Long>?): Flow<ZoneInventorySummaryResult> = flow {
        when (val filter = currentWarehouseAccessProvider.current().narrow(warehouseIds)) {
            WarehouseFilter.None -> return@flow
            WarehouseFilter.Unfiltered -> emitAll(zoneInventorySummaryRepository.findAll(null))
            is WarehouseFilter.In -> emitAll(zoneInventorySummaryRepository.findAll(filter.warehouseIds))
        }
    }
}
