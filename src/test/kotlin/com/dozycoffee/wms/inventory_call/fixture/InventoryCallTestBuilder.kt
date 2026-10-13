package com.dozycoffee.wms.inventory_call.fixture

import com.dozycoffee.wms.inventory_call.domain.enumeration.InventoryCallStatus
import com.dozycoffee.wms.inventory_call.domain.enumeration.InventoryCallType
import com.dozycoffee.wms.inventory_call.domain.model.InventoryCall
import java.time.LocalDateTime

class InventoryCallTestBuilder {

    private var inventoryCallId: Long? = null
    private var callType: InventoryCallType? = InventoryCallType.INBOUND_RECEIPT
    private var warehouseId: Long? = 1L
    private var idempotencyKey: String? = "wms-inbound-receipt-1"
    private var requestPath: String? = "/api/v1/inbound-receipts"
    private var requestBody: String? = "{\"quantity\":10}"
    private var status: InventoryCallStatus = InventoryCallStatus.PENDING
    private var attemptCount: Int = 0
    private var nextAttemptAt: LocalDateTime = NOW
    private var lastError: String? = null

    companion object {
        val NOW: LocalDateTime = LocalDateTime.of(2026, 10, 13, 10, 0, 0)

        fun inventoryCall(): InventoryCallTestBuilder = InventoryCallTestBuilder()
    }

    fun inventoryCallId(inventoryCallId: Long?): InventoryCallTestBuilder {
        this.inventoryCallId = inventoryCallId
        return this
    }

    fun callType(callType: InventoryCallType?): InventoryCallTestBuilder {
        this.callType = callType
        return this
    }

    fun warehouseId(warehouseId: Long?): InventoryCallTestBuilder {
        this.warehouseId = warehouseId
        return this
    }

    fun idempotencyKey(idempotencyKey: String?): InventoryCallTestBuilder {
        this.idempotencyKey = idempotencyKey
        return this
    }

    fun requestPath(requestPath: String?): InventoryCallTestBuilder {
        this.requestPath = requestPath
        return this
    }

    fun requestBody(requestBody: String?): InventoryCallTestBuilder {
        this.requestBody = requestBody
        return this
    }

    fun status(status: InventoryCallStatus): InventoryCallTestBuilder {
        this.status = status
        return this
    }

    fun attemptCount(attemptCount: Int): InventoryCallTestBuilder {
        this.attemptCount = attemptCount
        return this
    }

    fun nextAttemptAt(nextAttemptAt: LocalDateTime): InventoryCallTestBuilder {
        this.nextAttemptAt = nextAttemptAt
        return this
    }

    fun lastError(lastError: String?): InventoryCallTestBuilder {
        this.lastError = lastError
        return this
    }

    fun build(): InventoryCall {
        val id: Long? = inventoryCallId
        if (id != null) {
            return InventoryCall.reconstitute(
                id,
                requireNotNull(callType),
                requireNotNull(warehouseId),
                requireNotNull(idempotencyKey),
                requireNotNull(requestPath),
                requireNotNull(requestBody),
                status,
                attemptCount,
                nextAttemptAt,
                lastError,
                null,
                null,
                null
            )
        }
        return InventoryCall.create(callType, warehouseId, idempotencyKey, requestPath, requestBody, nextAttemptAt)
    }
}
