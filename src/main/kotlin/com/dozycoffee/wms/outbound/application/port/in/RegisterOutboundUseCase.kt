package com.dozycoffee.wms.outbound.application.port.`in`

import com.dozycoffee.wms.outbound.application.port.`in`.command.RegisterOutboundCommand
import com.dozycoffee.wms.outbound.application.port.`in`.result.OutboundResult

interface RegisterOutboundUseCase {
    suspend fun register(command: RegisterOutboundCommand): OutboundResult
}
