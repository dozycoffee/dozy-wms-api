package com.dozycoffee.wms.inbound.application.port.`in`

import com.dozycoffee.wms.inbound.application.port.`in`.result.InboundResult
import com.dozycoffee.wms.inbound.domain.enumeration.InboundStatus
import kotlinx.coroutines.flow.Flow

interface GetInboundUseCase {
    suspend fun getById(inboundId: Long): InboundResult
    fun getAll(status: InboundStatus?): Flow<InboundResult>
}
