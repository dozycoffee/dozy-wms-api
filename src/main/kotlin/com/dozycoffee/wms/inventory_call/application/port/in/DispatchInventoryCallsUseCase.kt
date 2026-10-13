package com.dozycoffee.wms.inventory_call.application.port.`in`

import com.dozycoffee.wms.inventory_call.application.port.`in`.result.DispatchInventoryCallsResult

interface DispatchInventoryCallsUseCase {
    /** 창고별로 보낼 차례인 호출을 한 건씩 전송하고 결과를 반영한다. 처리한 호출이 없으면 [DispatchInventoryCallsResult.processed]가 0이다 */
    suspend fun dispatchBatch(): DispatchInventoryCallsResult
}
