package com.dozycoffee.wms.inventory.application.port.out

import com.dozycoffee.wms.inventory.domain.enumeration.QualityStatus
import com.dozycoffee.wms.inventory.domain.model.Inventory
import kotlinx.coroutines.flow.Flow

interface InventoryRepository {
    suspend fun save(inventory: Inventory): Inventory
    suspend fun findById(inventoryId: Long): Inventory?
    fun findAll(locationId: Long?, productId: Long?, qualityStatus: QualityStatus?): Flow<Inventory>

    /** 유통기한 배치 스캔에서 만료된 Lot에 속한 재고를 찾기 위한 조회 */
    fun findAllByLotId(lotId: Long): Flow<Inventory>
}
