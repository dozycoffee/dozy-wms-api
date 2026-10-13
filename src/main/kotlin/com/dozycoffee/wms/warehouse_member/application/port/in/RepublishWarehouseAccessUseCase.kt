package com.dozycoffee.wms.warehouse_member.application.port.`in`

interface RepublishWarehouseAccessUseCase {
    /** 소속이 있는 모든 직원의 발행 대기를 새로 만든다. 처음 연동하거나 inventory 사본이 어긋났을 때 쓴다. 만든 직원 수를 돌려준다 */
    suspend fun republishAll(): Int
}
