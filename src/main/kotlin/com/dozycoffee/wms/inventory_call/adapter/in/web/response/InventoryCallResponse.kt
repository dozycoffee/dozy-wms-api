package com.dozycoffee.wms.inventory_call.adapter.`in`.web.response

import com.dozycoffee.wms.inventory_call.application.port.`in`.result.InventoryCallResult
import com.dozycoffee.wms.inventory_call.domain.enumeration.InventoryCallStatus
import com.dozycoffee.wms.inventory_call.domain.enumeration.InventoryCallType
import java.time.LocalDateTime

data class InventoryCallResponse(
    val inventoryCallId: Long,
    val callType: InventoryCallType,
    val warehouseId: Long,
    val idempotencyKey: String,
    val requestPath: String,
    val requestBody: String,
    val status: InventoryCallStatus,
    val attemptCount: Int,
    val nextAttemptAt: LocalDateTime,
    val lastError: String?,
    val responseBody: String?,
    val sentAt: LocalDateTime?,
    val deadAt: LocalDateTime?
) {
    companion object {
        fun from(result: InventoryCallResult): InventoryCallResponse {
            return InventoryCallResponse(
                result.inventoryCallId,
                result.callType,
                result.warehouseId,
                result.idempotencyKey,
                result.requestPath,
                result.requestBody,
                result.status,
                result.attemptCount,
                result.nextAttemptAt,
                result.lastError,
                result.responseBody,
                result.sentAt,
                result.deadAt
            )
        }
    }
}
