package com.dozycoffee.wms.inventory_call.domain.model

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.global.error.DomainValidator.requireNonNull
import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.inventory_call.domain.enumeration.InventoryCallStatus
import com.dozycoffee.wms.inventory_call.domain.enumeration.InventoryCallType
import com.dozycoffee.wms.inventory_call.domain.exception.InvalidInventoryCallStateException
import com.dozycoffee.wms.inventory_call.domain.exception.InventoryCallErrorCode
import java.time.LocalDateTime

/** inventory 서비스에 보낼 호출 한 건. 확정 트랜잭션이 기록하고 전송기가 창고별 순서대로 보낸다(ADR-0019) */
class InventoryCall private constructor(
    val inventoryCallId: Long?,
    val callType: InventoryCallType,
    val warehouseId: Long,
    val idempotencyKey: String,
    val requestPath: String,
    val requestBody: String,
    status: InventoryCallStatus,
    attemptCount: Int,
    nextAttemptAt: LocalDateTime,
    lastError: String?,
    responseBody: String?,
    sentAt: LocalDateTime?,
    deadAt: LocalDateTime?
) : BaseEntity() {

    var status: InventoryCallStatus = status
        private set

    var attemptCount: Int = attemptCount
        private set

    var nextAttemptAt: LocalDateTime = nextAttemptAt
        private set

    var lastError: String? = lastError
        private set

    var responseBody: String? = responseBody
        private set

    var sentAt: LocalDateTime? = sentAt
        private set

    var deadAt: LocalDateTime? = deadAt
        private set

    companion object {
        private const val MAX_KEY_LENGTH = 100
        private const val MAX_PATH_LENGTH = 200
        private const val MAX_ERROR_LENGTH = 500
        private val ASCII_PRINTABLE: IntRange = 0x21..0x7E

        fun create(
            callType: InventoryCallType?,
            warehouseId: Long?,
            idempotencyKey: String?,
            requestPath: String?,
            requestBody: String?,
            now: LocalDateTime
        ): InventoryCall {
            val validType: InventoryCallType = requireNonNull(callType, InventoryCallErrorCode.INVALID_CALL_TYPE)
            val validWarehouseId: Long = requireNonNull(warehouseId, InventoryCallErrorCode.INVALID_WAREHOUSE_ID)
            return InventoryCall(
                inventoryCallId = null,
                callType = validType,
                warehouseId = validWarehouseId,
                idempotencyKey = validateKey(idempotencyKey),
                requestPath = validatePath(requestPath),
                requestBody = validateBody(requestBody),
                status = InventoryCallStatus.PENDING,
                attemptCount = 0,
                nextAttemptAt = now,
                lastError = null,
                responseBody = null,
                sentAt = null,
                deadAt = null
            )
        }

        fun reconstitute(
            inventoryCallId: Long,
            callType: InventoryCallType,
            warehouseId: Long,
            idempotencyKey: String,
            requestPath: String,
            requestBody: String,
            status: InventoryCallStatus,
            attemptCount: Int,
            nextAttemptAt: LocalDateTime,
            lastError: String?,
            responseBody: String?,
            sentAt: LocalDateTime?,
            deadAt: LocalDateTime?
        ): InventoryCall {
            return InventoryCall(
                inventoryCallId, callType, warehouseId, idempotencyKey, requestPath, requestBody,
                status, attemptCount, nextAttemptAt, lastError, responseBody, sentAt, deadAt
            )
        }

        private fun validateKey(key: String?): String {
            if (key.isNullOrBlank() || key.length > MAX_KEY_LENGTH || key.any { it.code !in ASCII_PRINTABLE }) {
                throw InvalidDomainValueException(InventoryCallErrorCode.INVALID_IDEMPOTENCY_KEY)
            }
            return key
        }

        private fun validatePath(path: String?): String {
            if (path.isNullOrBlank() || !path.startsWith("/") || path.length > MAX_PATH_LENGTH) {
                throw InvalidDomainValueException(InventoryCallErrorCode.INVALID_REQUEST_PATH)
            }
            return path
        }

        private fun validateBody(body: String?): String {
            if (body.isNullOrBlank()) {
                throw InvalidDomainValueException(InventoryCallErrorCode.INVALID_REQUEST_BODY)
            }
            return body
        }

        private fun truncate(text: String?): String? = text?.take(MAX_ERROR_LENGTH)
    }

    /** 같은 키로 다시 기록하려는 호출이 이미 기록된 것과 같은 내용인지 본다 */
    fun hasSameRequest(callType: InventoryCallType, warehouseId: Long, requestPath: String, requestBody: String): Boolean {
        return this.callType == callType &&
            this.warehouseId == warehouseId &&
            this.requestPath == requestPath &&
            this.requestBody == requestBody
    }

    /** inventory가 2xx로 응답했다 */
    fun markSent(responseBody: String?, now: LocalDateTime) {
        requireStatus(InventoryCallStatus.PENDING)
        attemptCount += 1
        status = InventoryCallStatus.SENT
        this.responseBody = responseBody
        lastError = null
        sentAt = now
    }

    /** 재시도해 볼 만한 실패다. 다음 시도 시각을 미루고 PENDING을 유지한다 */
    fun retryLater(error: String, nextAttemptAt: LocalDateTime) {
        requireStatus(InventoryCallStatus.PENDING)
        attemptCount += 1
        lastError = truncate(error)
        this.nextAttemptAt = nextAttemptAt
    }

    /** 다시 보내도 결과가 달라지지 않는 실패라 격리한다. 운영자가 재전송하거나 버려야 한다 */
    fun markDead(error: String, responseBody: String?, now: LocalDateTime) {
        requireStatus(InventoryCallStatus.PENDING)
        attemptCount += 1
        status = InventoryCallStatus.DEAD
        lastError = truncate(error)
        this.responseBody = responseBody
        deadAt = now
    }

    /** 격리된 호출을 다시 전송 대기로 되돌린다 */
    fun requeue(now: LocalDateTime) {
        requireStatus(InventoryCallStatus.DEAD)
        status = InventoryCallStatus.PENDING
        attemptCount = 0
        nextAttemptAt = now
        lastError = null
        responseBody = null
        deadAt = null
    }

    /** 격리된 호출을 전송하지 않기로 하고 창고의 큐를 푼다 */
    fun discard() {
        requireStatus(InventoryCallStatus.DEAD)
        status = InventoryCallStatus.DISCARDED
    }

    private fun requireStatus(expected: InventoryCallStatus) {
        if (status != expected) {
            throw InvalidInventoryCallStateException()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InventoryCall) return false
        val id: Long? = inventoryCallId
        return id != null && id == other.inventoryCallId
    }

    override fun hashCode(): Int {
        return inventoryCallId?.hashCode() ?: System.identityHashCode(this)
    }
}
