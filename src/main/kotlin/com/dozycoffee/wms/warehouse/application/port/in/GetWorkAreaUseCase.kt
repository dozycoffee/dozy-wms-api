package com.dozycoffee.wms.warehouse.application.port.`in`

import com.dozycoffee.wms.warehouse.application.port.`in`.result.WorkAreaResult
import reactor.core.publisher.Mono

interface GetWorkAreaUseCase {
    fun getById(workAreaId: Long): Mono<WorkAreaResult>
}
