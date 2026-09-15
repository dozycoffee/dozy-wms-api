package com.dozycoffee.wms.disposal.application.port.`in`

import com.dozycoffee.wms.disposal.application.port.`in`.result.DisposalResult

interface CompleteDisposalUseCase {
    suspend fun complete(disposalId: Long): DisposalResult
}
