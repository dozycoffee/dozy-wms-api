package com.dozycoffee.wms.return_request.application.port.`in`

import com.dozycoffee.wms.return_request.application.port.`in`.command.InspectReturnItemCommand
import com.dozycoffee.wms.return_request.application.port.`in`.result.ReturnItemResult

interface InspectReturnItemUseCase {
    suspend fun inspect(command: InspectReturnItemCommand): ReturnItemResult
}
