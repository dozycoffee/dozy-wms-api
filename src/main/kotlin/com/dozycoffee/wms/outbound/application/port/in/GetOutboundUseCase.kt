package com.dozycoffee.wms.outbound.application.port.`in`

import com.dozycoffee.wms.outbound.application.port.`in`.result.OutboundResult
import com.dozycoffee.wms.outbound.domain.enumeration.OutboundStatus
import kotlinx.coroutines.flow.Flow

interface GetOutboundUseCase {
    suspend fun getById(outboundId: Long): OutboundResult
    fun getAll(status: OutboundStatus?): Flow<OutboundResult>
}
