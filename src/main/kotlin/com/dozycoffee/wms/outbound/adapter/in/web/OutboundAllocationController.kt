package com.dozycoffee.wms.outbound.adapter.`in`.web

import com.dozycoffee.wms.global.security.WmsAuthorize
import com.dozycoffee.wms.outbound.adapter.`in`.web.response.OutboundAllocationResponse
import com.dozycoffee.wms.outbound.application.port.`in`.GetOutboundAllocationUseCase
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/outbound-allocations")
class OutboundAllocationController(
    private val getOutboundAllocationUseCase: GetOutboundAllocationUseCase
) {

    @GetMapping
    @PreAuthorize(WmsAuthorize.READ)
    suspend fun getAllByOutbound(@RequestParam outboundId: Long): List<OutboundAllocationResponse> {
        return getOutboundAllocationUseCase.getAllocationsByOutbound(outboundId).map { OutboundAllocationResponse.from(it) }
    }
}
