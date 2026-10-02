package com.dozycoffee.wms.global.security

import org.springframework.stereotype.Component

/** 사용자-창고 매핑이 도입되기 전까지는 인증된 모든 행위자에게 전체 창고를 허용한다 */
@Component
class AllWarehousesAccessProvider : CurrentWarehouseAccessProvider {
    override suspend fun current(): WarehouseAccess = AllWarehouses
}
