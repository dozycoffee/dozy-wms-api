package com.dozycoffee.wms.inventory_call.application.port.`in`

import com.dozycoffee.wms.inventory_call.application.port.`in`.result.InventoryDirectCallResult

/** 아웃박스를 거치지 않고 inventory를 바로 호출한다. inventory의 판단을 기다려야 하는 확정(실사 조정)에만 쓴다 (ADR-0023) */
interface CallInventoryDirectlyUseCase {
    suspend fun post(path: String, idempotencyKey: String, body: String): InventoryDirectCallResult
}
