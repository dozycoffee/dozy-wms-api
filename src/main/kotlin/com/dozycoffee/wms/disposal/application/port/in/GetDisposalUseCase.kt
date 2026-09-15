package com.dozycoffee.wms.disposal.application.port.`in`

import com.dozycoffee.wms.disposal.application.port.`in`.result.DisposalResult
import com.dozycoffee.wms.disposal.domain.enumeration.DisposalStatus
import kotlinx.coroutines.flow.Flow

interface GetDisposalUseCase {
    suspend fun getById(disposalId: Long): DisposalResult
    fun getAll(status: DisposalStatus?): Flow<DisposalResult>
}
