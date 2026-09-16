package com.dozycoffee.wms.return_request.adapter.out.persistence

import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface ReturnRequestR2dbcRepository : CoroutineCrudRepository<ReturnRequestEntity, Long> {

    @Query(
        """
        SELECT * FROM return_request
        WHERE (:status IS NULL OR status = :status)
        """
    )
    fun findAllReturnRequests(status: String?): Flow<ReturnRequestEntity>
}
