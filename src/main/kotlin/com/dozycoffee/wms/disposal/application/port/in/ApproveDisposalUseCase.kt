package com.dozycoffee.wms.disposal.application.port.`in`

import com.dozycoffee.wms.disposal.application.port.`in`.result.DisposalResult

interface ApproveDisposalUseCase {
    suspend fun approve(disposalId: Long): DisposalResult
}
