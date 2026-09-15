package com.dozycoffee.wms.inbound.adapter.`in`.web

import com.dozycoffee.wms.inbound.adapter.`in`.web.request.InspectInboundItemRequest
import com.dozycoffee.wms.inbound.adapter.`in`.web.response.InboundItemResponse
import com.dozycoffee.wms.inbound.application.port.`in`.GetInboundItemUseCase
import com.dozycoffee.wms.inbound.application.port.`in`.InspectInboundItemUseCase
import jakarta.validation.Valid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/inbound-items")
class InboundItemController(
    private val inspectInboundItemUseCase: InspectInboundItemUseCase,
    private val getInboundItemUseCase: GetInboundItemUseCase
) {

    @GetMapping
    fun getAllByInbound(@RequestParam inboundId: Long): Flow<InboundItemResponse> {
        return getInboundItemUseCase.getAllByInbound(inboundId).map { InboundItemResponse.from(it) }
    }

    @PatchMapping("/{inboundItemId}/inspect")
    suspend fun inspect(
        @PathVariable inboundItemId: Long,
        @Valid @RequestBody request: InspectInboundItemRequest
    ): InboundItemResponse {
        return InboundItemResponse.from(inspectInboundItemUseCase.inspect(request.toCommand(inboundItemId)))
    }
}
