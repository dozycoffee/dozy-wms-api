package com.dozycoffee.wms.disposal.application.service

import com.dozycoffee.wms.disposal.application.port.`in`.GetDisposalItemUseCase
import com.dozycoffee.wms.disposal.application.port.`in`.result.DisposalItemResult
import com.dozycoffee.wms.disposal.application.port.out.DisposalItemRepository
import com.dozycoffee.wms.disposal.application.port.out.DisposalRepository
import com.dozycoffee.wms.disposal.domain.exception.DisposalNotFoundException
import com.dozycoffee.wms.global.security.WarehouseAccessGuard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DisposalItemService(
    private val disposalItemRepository: DisposalItemRepository,
    private val disposalRepository: DisposalRepository,
    private val warehouseAccessGuard: WarehouseAccessGuard
) : GetDisposalItemUseCase {

    @Transactional(readOnly = true)
    override fun getAllByDisposal(disposalId: Long): Flow<DisposalItemResult> {
        return flow {
            requireParentAccess(disposalId)
            emitAll(disposalItemRepository.findAllByDisposalId(disposalId).map { DisposalItemResult.from(it) })
        }
    }

    private suspend fun requireParentAccess(disposalId: Long) {
        val parent = disposalRepository.findById(disposalId) ?: throw DisposalNotFoundException()
        warehouseAccessGuard.require(parent.warehouseId)
    }
}
