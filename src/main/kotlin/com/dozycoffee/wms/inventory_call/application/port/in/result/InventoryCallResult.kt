package com.dozycoffee.wms.inventory_call.application.port.`in`.result

import com.dozycoffee.wms.inventory_call.domain.enumeration.InventoryCallStatus
import com.dozycoffee.wms.inventory_call.domain.enumeration.InventoryCallType
import com.dozycoffee.wms.inventory_call.domain.model.InventoryCall
import java.time.LocalDateTime

data class InventoryCallResult(
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
        fun from(call: InventoryCall): InventoryCallResult {
            return InventoryCallResult(
                requireNotNull(call.inventoryCallId),
                call.callType,
                call.warehouseId,
                call.idempotencyKey,
                call.requestPath,
                call.requestBody,
                call.status,
                call.attemptCount,
                call.nextAttemptAt,
                call.lastError,
                call.responseBody,
                call.sentAt,
                call.deadAt
            )
        }
    }
}
