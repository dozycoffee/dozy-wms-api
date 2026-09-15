package com.dozycoffee.wms.disposal.application.port.`in`

import com.dozycoffee.wms.disposal.application.port.`in`.result.DisposalItemResult
import kotlinx.coroutines.flow.Flow

interface GetDisposalItemUseCase {
    fun getAllByDisposal(disposalId: Long): Flow<DisposalItemResult>
}
