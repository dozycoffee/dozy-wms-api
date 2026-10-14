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
 * 시스템 작업과 [WmsRole.ADMIN], 서비스 간 호출([WmsRole.SERVICE])은 모든 창고를, 그 외 사용자는 배정된 창고만 접근한다.
 * [WmsRole.VIEWER]만 가진 사용자는 창고 마스터 조회 전용이라 모든 창고의 이름을 볼 수 있다. 창고 관리자 role을 함께 가지면 배정된 창고로 제한한다.
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
                if (seesAllWarehouses(actor.roles)) {
                    AllWarehouses
                } else {
                    OnlyWarehouses(warehouseMemberRepository.findWarehouseIdsByPrincipalId(actor.principalId))
                }
        }
    }

    private fun seesAllWarehouses(roles: Set<String>): Boolean =
        WmsRole.ADMIN.code in roles ||
            WmsRole.SERVICE.code in roles ||
            (WmsRole.VIEWER.code in roles && WmsRole.WAREHOUSE_MANAGER.code !in roles)
}
