package com.dozycoffee.wms.inbound.application.port.`in`

import com.dozycoffee.wms.inbound.application.port.`in`.command.CompleteInboundCommand
import com.dozycoffee.wms.inbound.application.port.`in`.result.InboundResult

interface CompleteInboundUseCase {
    suspend fun complete(command: CompleteInboundCommand): InboundResult
}
