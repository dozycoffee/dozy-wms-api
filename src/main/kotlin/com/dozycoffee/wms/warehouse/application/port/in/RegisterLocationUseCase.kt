package com.dozycoffee.wms.warehouse.application.port.`in`

import com.dozycoffee.wms.warehouse.application.port.`in`.command.RegisterLocationCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.result.LocationResult
import reactor.core.publisher.Mono

interface RegisterLocationUseCase {
    fun register(command: RegisterLocationCommand): Mono<LocationResult>
}
