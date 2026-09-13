package com.dozycoffee.wms.warehouse.application.port.out

import com.dozycoffee.wms.warehouse.domain.model.WorkArea
import reactor.core.publisher.Mono

interface WorkAreaRepository {
    fun save(workArea: WorkArea): Mono<WorkArea>
    fun findById(workAreaId: Long): Mono<WorkArea>
    fun delete(workArea: WorkArea): Mono<Void>
}
