package com.dozycoffee.wms.return_request.adapter.out.persistence

import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface ReturnItemR2dbcRepository : CoroutineCrudRepository<ReturnItemEntity, Long> {
    fun findAllByReturnRequestId(returnRequestId: Long): Flow<ReturnItemEntity>
}
