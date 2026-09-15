package com.dozycoffee.wms.disposal.application.port.`in`

import com.dozycoffee.wms.disposal.application.port.`in`.command.RegisterDisposalCommand
import com.dozycoffee.wms.disposal.application.port.`in`.result.DisposalResult

interface RegisterDisposalUseCase {
    suspend fun register(command: RegisterDisposalCommand): DisposalResult
}
