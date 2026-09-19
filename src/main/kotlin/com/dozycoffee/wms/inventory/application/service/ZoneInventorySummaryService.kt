package com.dozycoffee.wms.inventory.application.service

import com.dozycoffee.wms.global.security.CurrentAccessScopeProvider
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
    private val currentAccessScopeProvider: CurrentAccessScopeProvider
) : GetZoneInventorySummaryUseCase {

    @Transactional(readOnly = true)
    override fun getAll(warehouseIds: List<Long>?): Flow<ZoneInventorySummaryResult> = flow {
        val effectiveWarehouseIds = currentAccessScopeProvider.get().narrowWarehouseIds(warehouseIds)
        if (effectiveWarehouseIds != null && effectiveWarehouseIds.isEmpty()) return@flow
        emitAll(zoneInventorySummaryRepository.findAll(effectiveWarehouseIds))
    }
}
