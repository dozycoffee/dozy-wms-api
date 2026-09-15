package com.dozycoffee.wms.outbound.adapter.`in`.web

import com.dozycoffee.wms.outbound.adapter.`in`.web.response.OutboundItemResponse
import com.dozycoffee.wms.outbound.application.port.`in`.GetOutboundItemUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/outbound-items")
class OutboundItemController(
    private val getOutboundItemUseCase: GetOutboundItemUseCase
) {

    @GetMapping
    fun getAllByOutbound(@RequestParam outboundId: Long): Flow<OutboundItemResponse> {
        return getOutboundItemUseCase.getAllByOutbound(outboundId).map { OutboundItemResponse.from(it) }
    }
}
