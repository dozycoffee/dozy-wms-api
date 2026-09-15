package com.dozycoffee.wms.inbound.application.port.`in`

import com.dozycoffee.wms.inbound.application.port.`in`.command.InspectInboundItemCommand
import com.dozycoffee.wms.inbound.application.port.`in`.result.InboundItemResult

interface InspectInboundItemUseCase {
    suspend fun inspect(command: InspectInboundItemCommand): InboundItemResult
}
