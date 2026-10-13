package com.dozycoffee.wms.inventory_call.application.service

import com.dozycoffee.wms.inventory_call.application.port.`in`.DispatchInventoryCallsUseCase
import com.dozycoffee.wms.inventory_call.application.port.`in`.result.DispatchInventoryCallsResult
import com.dozycoffee.wms.inventory_call.application.port.out.InventoryApiClient
import com.dozycoffee.wms.inventory_call.application.port.out.InventoryApiResult
import com.dozycoffee.wms.inventory_call.application.port.out.InventoryCallRepository
import com.dozycoffee.wms.inventory_call.domain.model.InventoryCall
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime

@Service
class InventoryCallDispatchService(
    private val inventoryCallRepository: InventoryCallRepository,
    private val inventoryApiClient: InventoryApiClient,
    private val clock: Clock,
    @Value("\${wms.inventory-call.dispatch.batch-size:20}") private val batchSize: Int,
    @Value("\${wms.inventory-call.dispatch.base-backoff:PT5S}") private val baseBackoff: Duration,
    @Value("\${wms.inventory-call.dispatch.max-backoff:PT10M}") private val maxBackoff: Duration,
    @Value("\${wms.inventory-call.dispatch.alert-after-attempts:10}") private val alertAfterAttempts: Int
) : DispatchInventoryCallsUseCase {

    /**
     * 후보 행을 잠근 채 같은 트랜잭션에서 전송한다. 중간에 저장이 실패해 롤백되면 이미 보낸 호출이 다시 나가지만
     * inventory가 같은 멱등 키를 같은 결과로 처리하므로 안전하다.
     */
    @Transactional
    override suspend fun dispatchBatch(): DispatchInventoryCallsResult {
        val calls: List<InventoryCall> = inventoryCallRepository.findDispatchable(LocalDateTime.now(clock), batchSize).toList()
        var sent = 0
        var retrying = 0
        var dead = 0
        for (call in calls) {
            when (val outcome = inventoryApiClient.post(call.requestPath, call.idempotencyKey, call.requestBody)) {
                is InventoryApiResult.Success -> {
                    call.markSent(outcome.responseBody, LocalDateTime.now(clock))
                    sent++
                }
                is InventoryApiResult.Retryable -> {
                    val now = LocalDateTime.now(clock)
                    call.retryLater(outcome.reason, now.plus(backoff(call.attemptCount + 1)))
                    retrying++
                    alertIfStuck(call)
                }
                is InventoryApiResult.Permanent -> {
                    call.markDead(outcome.reason, outcome.responseBody, LocalDateTime.now(clock))
                    dead++
                    logger.error(
                        "INVENTORY_CALL_DEAD id={} type={} warehouseId={} key={} status={} reason={} body={}",
                        call.inventoryCallId, call.callType, call.warehouseId, call.idempotencyKey,
                        outcome.status, outcome.reason, outcome.responseBody
                    )
                }
            }
            inventoryCallRepository.save(call)
        }
        return DispatchInventoryCallsResult(sent, retrying, dead)
    }

    private fun backoff(attempt: Int): Duration {
        val exponent: Int = (attempt - 1).coerceIn(0, MAX_BACKOFF_EXPONENT)
        val delay: Duration = baseBackoff.multipliedBy(1L shl exponent)
        return if (delay > maxBackoff) maxBackoff else delay
    }

    private fun alertIfStuck(call: InventoryCall) {
        if (call.attemptCount >= alertAfterAttempts) {
            logger.error(
                "INVENTORY_CALL_RETRYING id={} type={} warehouseId={} key={} attempts={} reason={}",
                call.inventoryCallId, call.callType, call.warehouseId, call.idempotencyKey, call.attemptCount, call.lastError
            )
        }
    }

    companion object {
        private const val MAX_BACKOFF_EXPONENT = 20
        private val logger = LoggerFactory.getLogger(InventoryCallDispatchService::class.java)
    }
}
