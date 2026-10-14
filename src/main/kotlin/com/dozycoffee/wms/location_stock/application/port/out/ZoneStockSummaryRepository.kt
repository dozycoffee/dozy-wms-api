package com.dozycoffee.wms.location_stock.application.port.out

import com.dozycoffee.wms.location_stock.application.port.`in`.result.ZoneStockSummaryResult
import kotlinx.coroutines.flow.Flow

/**
 * Zone 단위 재고 현황 집계 — location_stock이 warehouse 도메인(zone/location) 테이블을 직접 조인해야 하는
 * 유일한 지점이다. 순수 조회 전용 프로젝션이라 장부 도메인 모델을 거치지 않고 결과를 바로
 * 반환한다.
 */
interface ZoneStockSummaryRepository {
    fun findAll(warehouseIds: List<Long>?): Flow<ZoneStockSummaryResult>
}
