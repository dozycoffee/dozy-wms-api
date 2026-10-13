package com.dozycoffee.wms.warehouse_member.adapter.`in`.scheduler

import com.dozycoffee.wms.warehouse_member.application.port.`in`.PublishWarehouseAccessEventsUseCase
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "wms.warehouse-access-event.publisher", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class WarehouseAccessEventScheduler(
    private val publishWarehouseAccessEventsUseCase: PublishWarehouseAccessEventsUseCase
) {

    @Scheduled(fixedDelayString = "\${wms.warehouse-access-event.publisher.interval:PT5S}")
    suspend fun publish() {
        var published = 0
        var failed = 0
        for (batch in 1..MAX_BATCHES_PER_RUN) {
            val result = publishWarehouseAccessEventsUseCase.publishBatch()
            published += result.published
            failed += result.failed
            if (result.processed == 0) break
        }
        if (published + failed > 0) {
            logger.info("직원 창고 소속 이벤트 발행: published={}, failed={}", published, failed)
        }
    }

    companion object {
        private const val MAX_BATCHES_PER_RUN = 50
        private val logger = LoggerFactory.getLogger(WarehouseAccessEventScheduler::class.java)
    }
}
