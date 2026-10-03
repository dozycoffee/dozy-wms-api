package com.dozycoffee.wms.support

import com.dozycoffee.wms.global.security.AllWarehouses
import com.dozycoffee.wms.global.security.CurrentWarehouseAccessProvider
import com.dozycoffee.wms.global.security.WarehouseAccess
import com.dozycoffee.wms.global.security.WarehouseAccessGuard

/** 서비스 테스트에서 창고 접근 범위를 고정하는 가드. 기본은 모든 창고 접근 가능이다 */
fun warehouseAccessGuardOf(access: WarehouseAccess = AllWarehouses): WarehouseAccessGuard =
    WarehouseAccessGuard(object : CurrentWarehouseAccessProvider {
        override suspend fun current(): WarehouseAccess = access
    })

/** 테스트 중에 접근 범위를 바꿀 수 있는 제공자 */
class SwitchableWarehouseAccess(var access: WarehouseAccess = AllWarehouses) : CurrentWarehouseAccessProvider {
    override suspend fun current(): WarehouseAccess = access
}
