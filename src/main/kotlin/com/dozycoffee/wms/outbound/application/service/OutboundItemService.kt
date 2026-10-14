package com.dozycoffee.wms.outbound.application.service

import com.dozycoffee.wms.global.security.WarehouseAccessGuard
import com.dozycoffee.wms.outbound.application.port.`in`.GetOutboundAllocationUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.GetOutboundItemUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.result.OutboundAllocationResult
import com.dozycoffee.wms.outbound.application.port.`in`.result.OutboundItemResult
import com.dozycoffee.wms.outbound.application.port.out.OutboundAllocationRepository
import com.dozycoffee.wms.outbound.application.port.out.OutboundItemRepository
import com.dozycoffee.wms.outbound.application.port.out.OutboundRepository
import com.dozycoffee.wms.outbound.domain.exception.OutboundNotFoundException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OutboundItemService(
    private val outboundItemRepository: OutboundItemRepository,
    private val outboundRepository: OutboundRepository,
    private val outboundAllocationRepository: OutboundAllocationRepository,
    private val warehouseAccessGuard: WarehouseAccessGuard
) : GetOutboundItemUseCase, GetOutboundAllocationUseCase {

    @Transactional(readOnly = true)
    override fun getAllByOutbound(outboundId: Long): Flow<OutboundItemResult> {
        return flow {
            requireParentAccess(outboundId)
            emitAll(outboundItemRepository.findAllByOutboundId(outboundId).map { OutboundItemResult.from(it) })
        }
    }

    @Transactional(readOnly = true)
    override suspend fun getAllocationsByOutbound(outboundId: Long): List<OutboundAllocationResult> {
        requireParentAccess(outboundId)
        val items = outboundItemRepository.findAllByOutboundId(outboundId).toList()
        val productIdByItemId = items.associate { requireNotNull(it.outboundItemId) to it.productId }
        return outboundAllocationRepository.findAllByOutboundItemIds(productIdByItemId.keys)
            .map { OutboundAllocationResult.from(it, productIdByItemId.getValue(it.outboundItemId)) }
    }

    private suspend fun requireParentAccess(outboundId: Long) {
        val parent = outboundRepository.findById(outboundId) ?: throw OutboundNotFoundException()
        warehouseAccessGuard.require(parent.warehouseId)
    }
}
