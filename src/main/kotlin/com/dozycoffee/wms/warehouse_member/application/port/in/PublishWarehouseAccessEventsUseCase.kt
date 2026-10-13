package com.dozycoffee.wms.warehouse_member.application.port.`in`

import com.dozycoffee.wms.warehouse_member.application.port.`in`.result.PublishWarehouseAccessEventsResult

interface PublishWarehouseAccessEventsUseCase {
    /** 보낼 차례인 직원 창고 소속 변경을 발행한다. 처리한 건이 없으면 [PublishWarehouseAccessEventsResult.processed]가 0이다 */
    suspend fun publishBatch(): PublishWarehouseAccessEventsResult
}
