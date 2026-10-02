package com.dozycoffee.wms.global.security

/** 현재 행위자가 접근할 수 있는 창고 범위. "전체"와 "없음"을 빈 리스트로 구분하지 않도록 타입으로 나눈다 */
sealed interface WarehouseAccess {

    /** 요청한 창고 ID(없으면 접근 가능한 전체)를 접근 범위로 좁힌 조회 조건 */
    fun narrow(requested: List<Long>?): WarehouseFilter

    fun canAccess(warehouseId: Long): Boolean
}

object AllWarehouses : WarehouseAccess {

    override fun narrow(requested: List<Long>?): WarehouseFilter =
        if (requested.isNullOrEmpty()) WarehouseFilter.Unfiltered else WarehouseFilter.In(requested)

    override fun canAccess(warehouseId: Long): Boolean = true
}

data class OnlyWarehouses(val warehouseIds: Set<Long>) : WarehouseAccess {

    override fun narrow(requested: List<Long>?): WarehouseFilter {
        val allowed: List<Long> = if (requested.isNullOrEmpty()) warehouseIds.toList() else requested.filter { it in warehouseIds }
        return if (allowed.isEmpty()) WarehouseFilter.None else WarehouseFilter.In(allowed)
    }

    override fun canAccess(warehouseId: Long): Boolean = warehouseId in warehouseIds
}

sealed interface WarehouseFilter {
    /** 창고 조건 없이 조회 */
    data object Unfiltered : WarehouseFilter

    /** 조회 결과가 항상 비므로 저장소를 호출하지 않는다 */
    data object None : WarehouseFilter

    /** [warehouseIds]는 비어 있지 않다 */
    data class In(val warehouseIds: List<Long>) : WarehouseFilter
}
