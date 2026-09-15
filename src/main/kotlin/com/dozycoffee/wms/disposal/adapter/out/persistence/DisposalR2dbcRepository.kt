package com.dozycoffee.wms.disposal.adapter.out.persistence

import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface DisposalR2dbcRepository : CoroutineCrudRepository<DisposalEntity, Long> {

    @Query(
        """
        SELECT * FROM disposal
        WHERE (:status IS NULL OR status = :status)
        """
    )
    fun findAllDisposals(status: String?): Flow<DisposalEntity>
}
