package com.dozycoffee.wms.inbound.adapter.`in`.web

import com.dozycoffee.wms.inbound.adapter.`in`.web.request.CompleteInboundRequest
import com.dozycoffee.wms.inbound.adapter.`in`.web.request.RegisterInboundRequest
import com.dozycoffee.wms.inbound.adapter.`in`.web.response.InboundResponse
import com.dozycoffee.wms.inbound.application.port.`in`.CompleteInboundUseCase
import com.dozycoffee.wms.inbound.application.port.`in`.GetInboundUseCase
import com.dozycoffee.wms.inbound.application.port.`in`.RegisterInboundUseCase
import com.dozycoffee.wms.inbound.application.port.`in`.StartInboundProcessingUseCase
import com.dozycoffee.wms.inbound.domain.enumeration.InboundStatus
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
@RequestMapping("/api/inbounds")
class InboundController(
    private val registerInboundUseCase: RegisterInboundUseCase,
    private val startInboundProcessingUseCase: StartInboundProcessingUseCase,
    private val completeInboundUseCase: CompleteInboundUseCase,
    private val getInboundUseCase: GetInboundUseCase
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun register(@Valid @RequestBody request: RegisterInboundRequest): InboundResponse {
        return InboundResponse.from(registerInboundUseCase.register(request.toCommand()))
    }

    @GetMapping("/{inboundId}")
    suspend fun getById(@PathVariable inboundId: Long): InboundResponse {
        return InboundResponse.from(getInboundUseCase.getById(inboundId))
    }

    @GetMapping
    fun getAll(@RequestParam(required = false) status: InboundStatus?): Flow<InboundResponse> {
        return getInboundUseCase.getAll(status).map { InboundResponse.from(it) }
    }

    @PatchMapping("/{inboundId}/processing")
    suspend fun startProcessing(@PathVariable inboundId: Long): InboundResponse {
        return InboundResponse.from(startInboundProcessingUseCase.startProcessing(inboundId))
    }

    @PatchMapping("/{inboundId}/complete")
    suspend fun complete(
        @PathVariable inboundId: Long,
        @Valid @RequestBody request: CompleteInboundRequest
    ): InboundResponse {
        return InboundResponse.from(completeInboundUseCase.complete(request.toCommand(inboundId)))
    }
}
