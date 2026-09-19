package com.dozycoffee.wms.inventory.application.port.`in`

import com.dozycoffee.wms.inventory.application.port.`in`.result.InventoryDetailResult
import com.dozycoffee.wms.inventory.application.port.`in`.result.InventoryResult
import com.dozycoffee.wms.inventory.domain.enumeration.QualityStatus
import kotlinx.coroutines.flow.Flow

interface GetInventoryUseCase {
    suspend fun getById(inventoryId: Long): InventoryResult

    /** 상세 조회 — 연결된 Lot 정보와 최근 재고 이력을 함께 반환 */
    suspend fun getDetailById(inventoryId: Long): InventoryDetailResult
    fun getAll(
        locationId: Long?,
        productId: Long?,
        qualityStatus: QualityStatus?,
        sortBy: InventorySortBy?,
        warehouseIds: List<Long>?
    ): Flow<InventoryResult>
}
