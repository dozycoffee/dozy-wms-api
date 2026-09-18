package com.dozycoffee.wms.inventory.application.service

import com.dozycoffee.wms.inventory.application.port.`in`.GetZoneInventorySummaryUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.result.ZoneInventorySummaryResult
import com.dozycoffee.wms.inventory.application.port.out.ZoneInventorySummaryRepository
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ZoneInventorySummaryService(
    private val zoneInventorySummaryRepository: ZoneInventorySummaryRepository
) : GetZoneInventorySummaryUseCase {

    @Transactional(readOnly = true)
    override fun getAll(): Flow<ZoneInventorySummaryResult> {
        return zoneInventorySummaryRepository.findAll()
    }
}
