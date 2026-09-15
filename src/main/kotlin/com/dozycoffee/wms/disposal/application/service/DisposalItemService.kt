package com.dozycoffee.wms.disposal.application.service

import com.dozycoffee.wms.disposal.application.port.`in`.GetDisposalItemUseCase
import com.dozycoffee.wms.disposal.application.port.`in`.result.DisposalItemResult
import com.dozycoffee.wms.disposal.application.port.out.DisposalItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DisposalItemService(
    private val disposalItemRepository: DisposalItemRepository
) : GetDisposalItemUseCase {

    @Transactional(readOnly = true)
    override fun getAllByDisposal(disposalId: Long): Flow<DisposalItemResult> {
        return disposalItemRepository.findAllByDisposalId(disposalId).map { DisposalItemResult.from(it) }
    }
}
