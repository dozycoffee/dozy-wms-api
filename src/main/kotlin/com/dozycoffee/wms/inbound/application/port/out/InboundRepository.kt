package com.dozycoffee.wms.inbound.application.port.out

import com.dozycoffee.wms.inbound.domain.enumeration.InboundStatus
import com.dozycoffee.wms.inbound.domain.model.Inbound
import kotlinx.coroutines.flow.Flow

interface InboundRepository {
    suspend fun save(inbound: Inbound): Inbound
    suspend fun findById(inboundId: Long): Inbound?
    fun findAll(status: InboundStatus?): Flow<Inbound>
}
