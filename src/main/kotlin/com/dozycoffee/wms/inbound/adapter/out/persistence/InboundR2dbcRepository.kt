package com.dozycoffee.wms.inbound.adapter.out.persistence

import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface InboundR2dbcRepository : CoroutineCrudRepository<InboundEntity, Long> {

    @Query(
        """
        SELECT * FROM inbound
        WHERE (:status IS NULL OR status = :status)
        """
    )
    fun findAllInbounds(status: String?): Flow<InboundEntity>
}
