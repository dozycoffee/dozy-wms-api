package com.dozycoffee.wms.inbound.application.port.`in`

import com.dozycoffee.wms.inbound.application.port.`in`.command.RegisterInboundCommand
import com.dozycoffee.wms.inbound.application.port.`in`.result.InboundResult

interface RegisterInboundUseCase {
    suspend fun register(command: RegisterInboundCommand): InboundResult
}
