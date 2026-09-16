package com.dozycoffee.wms.return_request.application.port.`in`

import com.dozycoffee.wms.return_request.application.port.`in`.command.RegisterReturnRequestCommand
import com.dozycoffee.wms.return_request.application.port.`in`.result.ReturnRequestResult

interface RegisterReturnRequestUseCase {
    suspend fun register(command: RegisterReturnRequestCommand): ReturnRequestResult
}
