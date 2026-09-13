package com.dozycoffee.wms.warehouse.application.port.`in`

import com.dozycoffee.wms.warehouse.application.port.`in`.command.RegisterZoneCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.result.ZoneResult
import reactor.core.publisher.Mono

interface RegisterZoneUseCase {
    fun register(command: RegisterZoneCommand): Mono<ZoneResult>
}
