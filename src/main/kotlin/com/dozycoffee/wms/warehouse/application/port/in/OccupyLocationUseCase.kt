package com.dozycoffee.wms.warehouse.application.port.`in`

import com.dozycoffee.wms.warehouse.application.port.`in`.command.OccupyLocationCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.result.LocationResult
import reactor.core.publisher.Mono

interface OccupyLocationUseCase {
    fun occupy(command: OccupyLocationCommand): Mono<LocationResult>
}
