package com.dozycoffee.wms.location_stock.application.port.out

import com.dozycoffee.wms.location_stock.domain.enumeration.StockQualityStatus
import com.dozycoffee.wms.location_stock.domain.model.LocationStock
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

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

    suspend fun findById(locationStockId: Long): LocationStock?

    /** 행을 잠그고 읽는다. 트랜잭션 안에서 호출해야 한다 */
    suspend fun findByIdForUpdate(locationStockId: Long): LocationStock?

    suspend fun findAllByIds(locationStockIds: Collection<Long>): List<LocationStock>

    /**
     * 창고에서 상품·Lot 번호·품질 상태가 같고 가용 수량이 남은 행을 잠그고 읽는다. 가용 수량이 많은 Location부터, 같으면 Location 코드
     * 오름차순이다. 트랜잭션 안에서 호출해야 한다.
     */
    suspend fun findAvailableForUpdate(
        warehouseId: Long,
        productId: Long,
        lotNumber: String,
        qualityStatus: StockQualityStatus
    ): List<LocationStock>

    /** 상품·Lot 번호의 행(수량 0 포함)이 하나라도 있는지 */
    suspend fun existsByProductIdAndLotNumber(productId: Long, lotNumber: String): Boolean

    /** [since] 이후에 행이 갱신됐는지 */
    suspend fun existsUpdatedAfter(locationStockId: Long, since: LocalDateTime): Boolean
}
