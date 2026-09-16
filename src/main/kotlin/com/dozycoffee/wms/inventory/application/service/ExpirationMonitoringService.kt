package com.dozycoffee.wms.inventory.application.service

import com.dozycoffee.wms.inventory.application.port.`in`.ScanExpirationUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.result.ExpirationScanResult
import com.dozycoffee.wms.inventory.application.port.out.InventoryRepository
import com.dozycoffee.wms.inventory.application.port.out.LotRepository
import com.dozycoffee.wms.inventory.domain.enumeration.LotStatus
import com.dozycoffee.wms.inventory.domain.enumeration.QualityStatus
import com.dozycoffee.wms.inventory.domain.exception.InventoryHasActiveAllocationException
import com.dozycoffee.wms.inventory.domain.model.Lot
import kotlinx.coroutines.flow.collect
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * 유통기한 배치 스캔 — CLAUDE.md "유통기한 모니터링" 규칙 구현체.
 *
 * 이미 점유(Allocation) 중인 재고가 만료 시점에 걸리면 [Inventory.markDisposalScheduled]가
 * `InventoryHasActiveAllocationException`을 던진다 — 진행 중인 출고 건의 점유를 스캔이 임의로
 * 해제하면 그 출고 흐름이 깨질 수 있으므로, 강제 해제 대신 건너뛰고 다음 스캔 주기에 재시도한다.
 */
@Service
class ExpirationMonitoringService(
    private val lotRepository: LotRepository,
    private val inventoryRepository: InventoryRepository
) : ScanExpirationUseCase {

    @Transactional
    override suspend fun scan(): ExpirationScanResult {
        val today = LocalDate.now()
        val expiringSoonThreshold = today.plusDays(EXPIRING_SOON_THRESHOLD_DAYS)

        var expiringSoonLotCount = 0
        var expiredLotCount = 0
        var disposalScheduledInventoryCount = 0
        var skippedActiveAllocationInventoryCount = 0

        lotRepository.findAllByLotStatusNotAndExpirationDateLessThanEqual(LotStatus.EXPIRED, expiringSoonThreshold)
            .collect { lot ->
                val expirationDate = lot.expirationDate ?: return@collect

                if (!expirationDate.isAfter(today)) {
                    if (lot.lotStatus != LotStatus.EXPIRED) {
                        lot.markExpired()
                        lotRepository.save(lot)
                        expiredLotCount++
                    }
                    val (scheduled, skipped) = scheduleDisposalForExpiredLot(lot)
                    disposalScheduledInventoryCount += scheduled
                    skippedActiveAllocationInventoryCount += skipped
                } else if (lot.lotStatus == LotStatus.NORMAL) {
                    lot.markExpiringSoon()
                    lotRepository.save(lot)
                    expiringSoonLotCount++
                }
            }

        return ExpirationScanResult(
            expiringSoonLotCount,
            expiredLotCount,
            disposalScheduledInventoryCount,
            skippedActiveAllocationInventoryCount
        )
    }

    private suspend fun scheduleDisposalForExpiredLot(lot: Lot): Pair<Int, Int> {
        val lotId = requireNotNull(lot.lotId)
        var scheduled = 0
        var skipped = 0

        inventoryRepository.findAllByLotId(lotId).collect { inventory ->
            if (inventory.qualityStatus == QualityStatus.NORMAL) {
                try {
                    inventory.markDisposalScheduled()
                    inventoryRepository.save(inventory)
                    scheduled++
                } catch (e: InventoryHasActiveAllocationException) {
                    skipped++
                }
            }
        }

        return scheduled to skipped
    }

    companion object {
        private const val EXPIRING_SOON_THRESHOLD_DAYS = 30L
    }
}
