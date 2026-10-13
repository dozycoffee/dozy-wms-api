package com.dozycoffee.wms.location_stock.application.port.out

import com.dozycoffee.wms.location_stock.domain.enumeration.StockQualityStatus
import com.dozycoffee.wms.location_stock.domain.model.LocationStock
import kotlinx.coroutines.flow.Flow

interface LocationStockRepository {
    suspend fun save(locationStock: LocationStock): LocationStock

    /** 같은 키에 대한 동시 갱신이 수량을 덮어쓰지 않도록 행을 잠그고 읽는다. 트랜잭션 안에서 호출해야 한다 */
    suspend fun findForUpdate(
        locationId: Long,
        productId: Long,
        lotNumber: String,
        qualityStatus: StockQualityStatus
    ): LocationStock?

    fun findAllByLocationId(locationId: Long): Flow<LocationStock>
}
