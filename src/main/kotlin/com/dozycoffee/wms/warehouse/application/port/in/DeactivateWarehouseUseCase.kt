package com.dozycoffee.wms.warehouse.application.port.`in`

import com.dozycoffee.wms.warehouse.application.port.`in`.result.WarehouseResult
import reactor.core.publisher.Mono

interface DeactivateWarehouseUseCase {
    fun deactivate(warehouseId: Long): Mono<WarehouseResult>
}
