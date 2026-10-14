package com.dozycoffee.wms.inventory_call.application.service

import com.dozycoffee.wms.inventory_call.application.port.`in`.CallInventoryDirectlyUseCase
import com.dozycoffee.wms.inventory_call.application.port.`in`.result.InventoryDirectCallResult
import com.dozycoffee.wms.inventory_call.application.port.out.InventoryApiClient
import com.dozycoffee.wms.inventory_call.application.port.out.InventoryApiResult
import org.springframework.stereotype.Service

@Service
class InventoryDirectCallService(
    private val inventoryApiClient: InventoryApiClient
) : CallInventoryDirectlyUseCase {

    override suspend fun post(path: String, idempotencyKey: String, body: String): InventoryDirectCallResult {
        return when (val result = inventoryApiClient.post(path, idempotencyKey, body)) {
            is InventoryApiResult.Success -> InventoryDirectCallResult.Accepted(result.responseBody)
            is InventoryApiResult.Permanent -> InventoryDirectCallResult.Rejected(result.status, result.responseBody)
            is InventoryApiResult.Retryable -> InventoryDirectCallResult.Unavailable(result.reason)
        }
    }
}
