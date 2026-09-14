package com.dozycoffee.wms.inventory.application.port.`in`

import com.dozycoffee.wms.inventory.application.port.`in`.command.RegisterLotCommand
import com.dozycoffee.wms.inventory.application.port.`in`.result.LotResult

interface RegisterLotUseCase {
    suspend fun register(command: RegisterLotCommand): LotResult
}
