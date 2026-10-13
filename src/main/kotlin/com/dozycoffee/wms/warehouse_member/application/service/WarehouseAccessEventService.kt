package com.dozycoffee.wms.warehouse_member.application.service

import com.dozycoffee.wms.global.event.UuidV7
import com.dozycoffee.wms.warehouse_member.application.port.`in`.PublishWarehouseAccessEventsUseCase
import com.dozycoffee.wms.warehouse_member.application.port.`in`.RepublishWarehouseAccessUseCase
import com.dozycoffee.wms.warehouse_member.application.port.`in`.result.PublishWarehouseAccessEventsResult
import com.dozycoffee.wms.warehouse_member.application.port.out.WarehouseAccessEventPublisher
import com.dozycoffee.wms.warehouse_member.application.port.out.WarehouseAccessEventRepository
import com.dozycoffee.wms.warehouse_member.application.port.out.WarehouseMemberRepository
import com.dozycoffee.wms.warehouse_member.domain.model.WarehouseAccessEvent
import kotlinx.coroutines.CancellationException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime

@Service
class WarehouseAccessEventService(
    private val warehouseAccessEventRepository: WarehouseAccessEventRepository,
    private val warehouseMemberRepository: WarehouseMemberRepository,
    private val warehouseAccessEventPublisher: WarehouseAccessEventPublisher,
    private val transactionalOperator: TransactionalOperator,
    private val clock: Clock,
    @Value("\${wms.warehouse-access-event.publisher.batch-size:50}") private val batchSize: Int,
    @Value("\${wms.warehouse-access-event.publisher.lease:PT1M}") private val lease: Duration,
    @Value("\${wms.warehouse-access-event.publisher.base-backoff:PT5S}") private val baseBackoff: Duration,
    @Value("\${wms.warehouse-access-event.publisher.max-backoff:PT5M}") private val maxBackoff: Duration,
    @Value("\${wms.warehouse-access-event.publisher.alert-after-attempts:10}") private val alertAfterAttempts: Int
) : PublishWarehouseAccessEventsUseCase, RepublishWarehouseAccessUseCase {

    /**
     * 행을 짧은 트랜잭션에서 점유(lease)한 뒤 트랜잭션 밖에서 보낸다. Kafka 확인을 기다리는 동안 행 잠금을 잡지 않아 소속 변경 요청이 막히지 않는다.
     * 보내는 중에 서비스가 죽으면 lease가 끝난 뒤 같은 `eventId`로 다시 나간다.
     */
    override suspend fun publishBatch(): PublishWarehouseAccessEventsResult {
        val now = LocalDateTime.now(clock)
        val claimed: List<WarehouseAccessEvent> =
            transactionalOperator.executeAndAwait { warehouseAccessEventRepository.claimDue(now, now.plus(lease), batchSize) }
                ?: emptyList()
        var published = 0
        var failed = 0
        for (event in claimed) {
            try {
                val warehouseIds: List<Long> = warehouseMemberRepository.findWarehouseIdsByPrincipalId(event.principalId).sorted()
                warehouseAccessEventPublisher.publish(event.principalId, event.eventId, event.occurredAt, warehouseIds)
                warehouseAccessEventRepository.deleteIfUnchanged(event.principalId, event.eventId)
                published++
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val attempts: Int = event.attemptCount + 1
                warehouseAccessEventRepository.recordFailure(
                    event.principalId,
                    event.eventId,
                    attempts,
                    "${e.javaClass.simpleName}: ${e.message}",
                    LocalDateTime.now(clock).plus(backoff(attempts))
                )
                failed++
                if (attempts >= alertAfterAttempts) {
                    logger.error(
                        "WAREHOUSE_ACCESS_EVENT_RETRYING principalId={} eventId={} attempts={} reason={}",
                        event.principalId, event.eventId, attempts, e.message
                    )
                }
            }
        }
        return PublishWarehouseAccessEventsResult(published, failed)
    }

    @Transactional
    override suspend fun republishAll(): Int {
        val principalIds = warehouseMemberRepository.findAllPrincipalIds()
        val now = LocalDateTime.now(clock)
        principalIds.forEach { principalId ->
            warehouseAccessEventRepository.markChanged(principalId, UuidV7.generate(clock.millis()), now, now)
        }
        return principalIds.size
    }

    private fun backoff(attempt: Int): Duration {
        val exponent: Int = (attempt - 1).coerceIn(0, MAX_BACKOFF_EXPONENT)
        val delay: Duration = baseBackoff.multipliedBy(1L shl exponent)
        return if (delay > maxBackoff) maxBackoff else delay
    }

    companion object {
        private const val MAX_BACKOFF_EXPONENT = 20
        private val logger = LoggerFactory.getLogger(WarehouseAccessEventService::class.java)
    }
}
