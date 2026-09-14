package com.dozycoffee.wms.inventory.application.port.out

import com.dozycoffee.wms.inventory.domain.model.Lot
import kotlinx.coroutines.flow.Flow

interface LotRepository {
    suspend fun save(lot: Lot): Lot
    suspend fun findById(lotId: Long): Lot?
    suspend fun existsByProductIdAndLotNumber(productId: Long, lotNumber: String): Boolean
    fun findAllByProductId(productId: Long): Flow<Lot>
}
