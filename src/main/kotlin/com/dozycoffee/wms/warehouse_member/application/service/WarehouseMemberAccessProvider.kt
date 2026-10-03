package com.dozycoffee.wms.warehouse_member.application.service

import com.dozycoffee.wms.global.security.AllWarehouses
import com.dozycoffee.wms.global.security.CurrentActorProvider
import com.dozycoffee.wms.global.security.CurrentWarehouseAccessProvider
import com.dozycoffee.wms.global.security.OnlyWarehouses
import com.dozycoffee.wms.global.security.SystemActor
import com.dozycoffee.wms.global.security.UserActor
import com.dozycoffee.wms.global.security.WarehouseAccess
import com.dozycoffee.wms.global.security.WmsRole
import com.dozycoffee.wms.warehouse_member.application.port.out.WarehouseMemberRepository
import org.springframework.stereotype.Component

/**
 * 시스템 작업과 [WmsRole.WAREHOUSE_ADMIN]은 모든 창고를, 그 외 사용자는 배정된 창고만 접근한다.
 * 배정이 없는 사용자는 접근 가능한 창고가 없다(fail-closed).
 */
@Component
class WarehouseMemberAccessProvider(
    private val currentActorProvider: CurrentActorProvider,
    private val warehouseMemberRepository: WarehouseMemberRepository
) : CurrentWarehouseAccessProvider {

    override suspend fun current(): WarehouseAccess {
        return when (val actor = currentActorProvider.get()) {
            SystemActor -> AllWarehouses
            is UserActor ->
                if (WmsRole.WAREHOUSE_ADMIN.code in actor.roles) {
                    AllWarehouses
                } else {
                    OnlyWarehouses(warehouseMemberRepository.findWarehouseIdsByPrincipalId(actor.principalId))
                }
        }
    }
}
