package com.dozycoffee.wms.outbound.application.port.out

import com.dozycoffee.wms.outbound.domain.enumeration.OutboundStatus
import com.dozycoffee.wms.outbound.domain.model.Outbound
import kotlinx.coroutines.flow.Flow

interface OutboundRepository {
    suspend fun save(outbound: Outbound): Outbound
    suspend fun findById(outboundId: Long): Outbound?
    fun findAll(status: OutboundStatus?): Flow<Outbound>
}
