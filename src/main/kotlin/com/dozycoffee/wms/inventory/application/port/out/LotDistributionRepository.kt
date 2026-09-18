package com.dozycoffee.wms.inventory.application.port.out

import com.dozycoffee.wms.inventory.application.port.`in`.result.LotDistributionResult
import kotlinx.coroutines.flow.Flow

/**
 * Lot 상세 조회에서 Zone/Location별 재고 분포를 보여주기 위한 조회 — zone-summary와 마찬가지로
 * inventory가 warehouse 도메인(zone/location) 테이블을 직접 조인해야 하는 순수 조회 전용 프로젝션.
 */
interface LotDistributionRepository {
    fun findAllByLotId(lotId: Long): Flow<LotDistributionResult>
}
