package com.dozycoffee.wms.inventory.application.port.`in`

import com.dozycoffee.wms.inventory.application.port.`in`.result.LotResult
import kotlinx.coroutines.flow.Flow

interface GetLotUseCase {
    suspend fun getById(lotId: Long): LotResult
    fun getAllByProduct(productId: Long): Flow<LotResult>
}
