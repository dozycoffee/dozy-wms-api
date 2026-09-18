package com.dozycoffee.wms.inventory.application.port.`in`

import com.dozycoffee.wms.inventory.application.port.`in`.result.ZoneInventorySummaryResult
import kotlinx.coroutines.flow.Flow

interface GetZoneInventorySummaryUseCase {
    fun getAll(): Flow<ZoneInventorySummaryResult>
}
