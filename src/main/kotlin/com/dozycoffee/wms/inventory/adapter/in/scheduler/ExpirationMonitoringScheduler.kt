package com.dozycoffee.wms.inventory.adapter.`in`.scheduler

import com.dozycoffee.wms.inventory.application.port.`in`.ScanExpirationUseCase
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ExpirationMonitoringScheduler(
    private val scanExpirationUseCase: ScanExpirationUseCase
) {

    @Scheduled(cron = "\${wms.expiration-monitoring.cron:0 0 1 * * *}")
    suspend fun scan() {
        val result = scanExpirationUseCase.scan()
        logger.info(
            "유통기한 배치 스캔 완료: expiringSoon={}, expired={}, disposalScheduled={}, skippedActiveAllocation={}",
            result.expiringSoonLotCount,
            result.expiredLotCount,
            result.disposalScheduledInventoryCount,
            result.skippedActiveAllocationInventoryCount
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ExpirationMonitoringScheduler::class.java)
    }
}
