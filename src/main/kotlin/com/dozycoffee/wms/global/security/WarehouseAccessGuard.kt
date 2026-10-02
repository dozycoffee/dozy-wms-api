package com.dozycoffee.wms.global.security

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
}
