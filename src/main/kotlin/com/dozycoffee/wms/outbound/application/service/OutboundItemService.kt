package com.dozycoffee.wms.outbound.application.service

import com.dozycoffee.wms.outbound.application.port.`in`.GetOutboundItemUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.result.OutboundItemResult
import com.dozycoffee.wms.outbound.application.port.out.OutboundItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OutboundItemService(
    private val outboundItemRepository: OutboundItemRepository
) : GetOutboundItemUseCase {

    @Transactional(readOnly = true)
    override fun getAllByOutbound(outboundId: Long): Flow<OutboundItemResult> {
        return outboundItemRepository.findAllByOutboundId(outboundId).map { OutboundItemResult.from(it) }
    }
}
