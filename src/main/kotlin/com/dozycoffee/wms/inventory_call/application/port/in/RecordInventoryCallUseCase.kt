package com.dozycoffee.wms.inventory_call.application.port.`in`

import com.dozycoffee.wms.inventory_call.application.port.`in`.command.RecordInventoryCallCommand
import com.dozycoffee.wms.inventory_call.application.port.`in`.result.InventoryCallResult

interface RecordInventoryCallUseCase {
    /** 호출을 아웃박스에 기록한다. 호출한 확정 트랜잭션에 참여해야 하며, 같은 키·같은 내용의 재기록은 기존 행을 돌려준다 */
    suspend fun record(command: RecordInventoryCallCommand): InventoryCallResult
}
