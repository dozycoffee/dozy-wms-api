package com.dozycoffee.wms.inbound.application.service

import com.dozycoffee.wms.global.security.WarehouseAccessGuard
import com.dozycoffee.wms.inbound.application.port.`in`.GetInboundItemUseCase
import com.dozycoffee.wms.inbound.application.port.`in`.InspectInboundItemUseCase
import com.dozycoffee.wms.inbound.application.port.`in`.command.InspectInboundItemCommand
import com.dozycoffee.wms.inbound.application.port.`in`.result.InboundItemResult
import com.dozycoffee.wms.inbound.application.port.out.InboundItemRepository
import com.dozycoffee.wms.inbound.application.port.out.InboundRepository
import com.dozycoffee.wms.inbound.domain.exception.InboundItemNotFoundException
import com.dozycoffee.wms.inbound.domain.exception.InboundNotFoundException
import com.dozycoffee.wms.inbound.domain.model.InboundItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class InboundItemService(
    private val inboundItemRepository: InboundItemRepository,
    private val inboundRepository: InboundRepository,
    private val warehouseAccessGuard: WarehouseAccessGuard
) : InspectInboundItemUseCase, GetInboundItemUseCase {

    @Transactional
    override suspend fun inspect(command: InspectInboundItemCommand): InboundItemResult {
        val inboundItem = findInboundItemOrThrow(command.inboundItemId)
        requireParentAccess(inboundItem.inboundId)
        inboundItem.inspect(command.actualQuantity, command.inspectionResult)
        return InboundItemResult.from(inboundItemRepository.save(inboundItem))
    }

    @Transactional(readOnly = true)
    override fun getAllByInbound(inboundId: Long): Flow<InboundItemResult> {
        return flow {
            requireParentAccess(inboundId)
            emitAll(inboundItemRepository.findAllByInboundId(inboundId).map { InboundItemResult.from(it) })
        }
    }

    private suspend fun findInboundItemOrThrow(inboundItemId: Long): InboundItem {
        return inboundItemRepository.findById(inboundItemId) ?: throw InboundItemNotFoundException()
    }

    private suspend fun requireParentAccess(inboundId: Long) {
        val parent = inboundRepository.findById(inboundId) ?: throw InboundNotFoundException()
        warehouseAccessGuard.require(parent.warehouseId)
    }
}
