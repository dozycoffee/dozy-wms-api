package com.dozycoffee.wms.inventory_call.adapter.`in`.scheduler

import com.dozycoffee.wms.inventory_call.application.port.`in`.DispatchInventoryCallsUseCase
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["wms.inventory-call.dispatch.enabled"], havingValue = "true", matchIfMissing = true)
class InventoryCallDispatchScheduler(
    private val dispatchInventoryCallsUseCase: DispatchInventoryCallsUseCase
) {

    @Scheduled(fixedDelayString = "\${wms.inventory-call.dispatch.interval:PT5S}")
    suspend fun dispatch() {
        var sent = 0
        var retrying = 0
        var dead = 0
        for (batch in 1..MAX_BATCHES_PER_RUN) {
            val result = dispatchInventoryCallsUseCase.dispatchBatch()
            sent += result.sent
            retrying += result.retrying
            dead += result.dead
            if (result.processed == 0) break
        }
        if (sent + retrying + dead > 0) {
            logger.info("inventory 호출 전송: sent={}, retrying={}, dead={}", sent, retrying, dead)
        }
    }

    companion object {
        private const val MAX_BATCHES_PER_RUN = 50
        private val logger = LoggerFactory.getLogger(InventoryCallDispatchScheduler::class.java)
    }
}
