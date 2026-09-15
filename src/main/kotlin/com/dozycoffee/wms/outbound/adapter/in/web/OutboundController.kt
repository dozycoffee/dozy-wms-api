package com.dozycoffee.wms.outbound.adapter.`in`.web

import com.dozycoffee.wms.outbound.adapter.`in`.web.request.RegisterOutboundRequest
import com.dozycoffee.wms.outbound.adapter.`in`.web.response.OutboundResponse
import com.dozycoffee.wms.outbound.application.port.`in`.CompleteOutboundUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.GetOutboundUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.RegisterOutboundUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.StartOutboundInspectingUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.StartOutboundPickingUseCase
import com.dozycoffee.wms.outbound.domain.enumeration.OutboundStatus
import jakarta.validation.Valid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/outbounds")
class OutboundController(
    private val registerOutboundUseCase: RegisterOutboundUseCase,
    private val startOutboundPickingUseCase: StartOutboundPickingUseCase,
    private val startOutboundInspectingUseCase: StartOutboundInspectingUseCase,
    private val completeOutboundUseCase: CompleteOutboundUseCase,
    private val getOutboundUseCase: GetOutboundUseCase
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun register(@Valid @RequestBody request: RegisterOutboundRequest): OutboundResponse {
        return OutboundResponse.from(registerOutboundUseCase.register(request.toCommand()))
    }

    @GetMapping("/{outboundId}")
    suspend fun getById(@PathVariable outboundId: Long): OutboundResponse {
        return OutboundResponse.from(getOutboundUseCase.getById(outboundId))
    }

    @GetMapping
    fun getAll(@RequestParam(required = false) status: OutboundStatus?): Flow<OutboundResponse> {
        return getOutboundUseCase.getAll(status).map { OutboundResponse.from(it) }
    }

    @PatchMapping("/{outboundId}/picking")
    suspend fun startPicking(@PathVariable outboundId: Long): OutboundResponse {
        return OutboundResponse.from(startOutboundPickingUseCase.startPicking(outboundId))
    }

    @PatchMapping("/{outboundId}/inspecting")
    suspend fun startInspecting(@PathVariable outboundId: Long): OutboundResponse {
        return OutboundResponse.from(startOutboundInspectingUseCase.startInspecting(outboundId))
    }

    @PatchMapping("/{outboundId}/complete")
    suspend fun complete(@PathVariable outboundId: Long): OutboundResponse {
        return OutboundResponse.from(completeOutboundUseCase.complete(outboundId))
    }
}
