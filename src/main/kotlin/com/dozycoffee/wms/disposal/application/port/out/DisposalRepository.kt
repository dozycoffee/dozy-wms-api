package com.dozycoffee.wms.disposal.application.port.out

import com.dozycoffee.wms.disposal.domain.enumeration.DisposalStatus
import com.dozycoffee.wms.disposal.domain.model.Disposal
import kotlinx.coroutines.flow.Flow

interface DisposalRepository {
    suspend fun save(disposal: Disposal): Disposal
    suspend fun findById(disposalId: Long): Disposal?
    fun findAll(status: DisposalStatus?): Flow<Disposal>
}
