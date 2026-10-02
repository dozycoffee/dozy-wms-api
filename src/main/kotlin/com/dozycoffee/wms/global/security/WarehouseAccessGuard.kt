package com.dozycoffee.wms.global.security

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.stereotype.Component

/** 서비스 계층이 현재 행위자의 창고 접근 범위를 검사하는 진입점이다 */
@Component
class WarehouseAccessGuard(
    private val currentWarehouseAccessProvider: CurrentWarehouseAccessProvider
) {

    suspend fun require(warehouseId: Long) {
        if (!currentWarehouseAccessProvider.current().canAccess(warehouseId)) {
            throw WarehouseAccessDeniedException()
        }
    }

    suspend fun narrow(requested: List<Long>?): WarehouseFilter =
        currentWarehouseAccessProvider.current().narrow(requested)

    /**
     * 접근 범위로 좁힌 창고 ID 조건으로 조회한다. [query]에 전달되는 null은 창고 조건 없음을 뜻한다.
     * 접근 가능한 창고가 없으면 [query]를 호출하지 않고 빈 결과를 반환한다.
     */
    fun <T> scoped(requested: List<Long>? = null, query: (List<Long>?) -> Flow<T>): Flow<T> = flow {
        when (val filter = narrow(requested)) {
            WarehouseFilter.None -> Unit
            WarehouseFilter.Unfiltered -> emitAll(query(null))
            is WarehouseFilter.In -> emitAll(query(filter.warehouseIds))
        }
    }
}
