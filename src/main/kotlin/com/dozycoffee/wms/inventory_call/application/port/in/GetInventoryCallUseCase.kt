package com.dozycoffee.wms.inventory_call.application.port.`in`

import com.dozycoffee.wms.inventory_call.application.port.`in`.result.InventoryCallResult
import com.dozycoffee.wms.inventory_call.domain.enumeration.InventoryCallStatus
import kotlinx.coroutines.flow.Flow

interface GetInventoryCallUseCase {
    suspend fun getById(inventoryCallId: Long): InventoryCallResult

    fun getAll(status: InventoryCallStatus?, limit: Int): Flow<InventoryCallResult>
}
