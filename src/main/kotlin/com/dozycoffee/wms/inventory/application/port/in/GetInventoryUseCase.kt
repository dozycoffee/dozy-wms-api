package com.dozycoffee.wms.inventory.application.port.`in`

import com.dozycoffee.wms.inventory.application.port.`in`.result.InventoryResult
import com.dozycoffee.wms.inventory.domain.enumeration.QualityStatus
import kotlinx.coroutines.flow.Flow

interface GetInventoryUseCase {
    suspend fun getById(inventoryId: Long): InventoryResult
    fun getAll(
        locationId: Long?,
        productId: Long?,
        qualityStatus: QualityStatus?,
        sortBy: InventorySortBy?
    ): Flow<InventoryResult>
}
