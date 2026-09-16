package com.dozycoffee.wms.inventory.application.port.out

import com.dozycoffee.wms.inventory.domain.enumeration.LotStatus
import com.dozycoffee.wms.inventory.domain.model.Lot
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface LotRepository {
    suspend fun save(lot: Lot): Lot
    suspend fun findById(lotId: Long): Lot?
    suspend fun existsByProductIdAndLotNumber(productId: Long, lotNumber: String): Boolean
    fun findAllByProductId(productId: Long): Flow<Lot>

    /** 유통기한 배치 스캔 대상 조회 — 아직 EXPIRED가 아니면서 유통기한이 threshold 이내로 다가온 Lot 전체 */
    fun findAllByLotStatusNotAndExpirationDateLessThanEqual(lotStatus: LotStatus, threshold: LocalDate): Flow<Lot>
}
