package com.dozycoffee.wms.disposal.adapter.out.persistence

import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface DisposalItemR2dbcRepository : CoroutineCrudRepository<DisposalItemEntity, Long> {
    fun findAllByDisposalId(disposalId: Long): Flow<DisposalItemEntity>
}
