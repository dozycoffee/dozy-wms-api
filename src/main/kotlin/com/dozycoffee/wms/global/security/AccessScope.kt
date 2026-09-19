package com.dozycoffee.wms.global.security

data class AccessScope(val userId: String, val warehouseIds: List<Long> = emptyList()) {

    /** 반환값이 빈 리스트면 "접근 가능한 창고 없음"이므로, 리포지토리의 null/빈 리스트=전체조회 의미론과 혼동해 그대로 넘기면 안 된다 */
    fun narrowWarehouseIds(requested: List<Long>?): List<Long>? {
        if (warehouseIds.isEmpty()) return requested
        if (requested.isNullOrEmpty()) return warehouseIds
        return requested.filter { it in warehouseIds }
    }
}
