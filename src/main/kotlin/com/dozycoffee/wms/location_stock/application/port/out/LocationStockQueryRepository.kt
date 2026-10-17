package com.dozycoffee.wms.location_stock.application.port.out

import com.dozycoffee.wms.location_stock.application.port.`in`.query.LocationStockQuery
import com.dozycoffee.wms.location_stock.application.port.`in`.result.LocationStockDetailResult
import kotlinx.coroutines.flow.Flow

/**
 * 장부 행에 Location·Zone 정보를 붙여 읽는 조회 전용 프로젝션 — [ZoneStockSummaryRepository]처럼 warehouse 도메인 테이블을 조인하므로
 * 장부 도메인 모델을 거치지 않는다. [LocationStockQuery.warehouseIds]는 접근 범위로 좁힌 값이고 null이면 창고 조건이 없다.
 */
interface LocationStockQueryRepository {
    fun findAll(query: LocationStockQuery): Flow<LocationStockDetailResult>
}
