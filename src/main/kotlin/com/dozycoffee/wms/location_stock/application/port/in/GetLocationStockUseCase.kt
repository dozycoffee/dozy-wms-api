package com.dozycoffee.wms.location_stock.application.port.`in`

import com.dozycoffee.wms.location_stock.application.port.`in`.result.LocationStockResult
import java.time.LocalDateTime

interface GetLocationStockUseCase {
    suspend fun getById(locationStockId: Long): LocationStockResult

    suspend fun getAllByIds(locationStockIds: Collection<Long>): List<LocationStockResult>

    /** Location의 모든 행(수량 0 포함) */
    suspend fun getAllByLocationId(locationId: Long): List<LocationStockResult>

    /** 상품·Lot 번호의 행이 하나라도 있는지(수량 0 포함). Lot은 입고로만 생기고 입고는 장부에 반영되므로 등록된 Lot인지의 근거가 된다 */
    suspend fun existsLot(productId: Long, lotNumber: String): Boolean

    /** [since] 이후에 행이 바뀌었는지(입출고·품질 전환·점유 등) */
    suspend fun hasChangedSince(locationStockId: Long, since: LocalDateTime): Boolean
}
