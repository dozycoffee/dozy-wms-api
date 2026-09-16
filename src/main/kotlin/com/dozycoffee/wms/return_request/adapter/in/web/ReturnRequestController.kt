package com.dozycoffee.wms.return_request.adapter.`in`.web

import com.dozycoffee.wms.return_request.adapter.`in`.web.request.CompleteReturnRequestRequest
import com.dozycoffee.wms.return_request.adapter.`in`.web.request.RegisterReturnRequestRequest
import com.dozycoffee.wms.return_request.adapter.`in`.web.response.ReturnRequestResponse
import com.dozycoffee.wms.return_request.application.port.`in`.CompleteReturnRequestUseCase
import com.dozycoffee.wms.return_request.application.port.`in`.GetReturnRequestUseCase
import com.dozycoffee.wms.return_request.application.port.`in`.RegisterReturnRequestUseCase
import com.dozycoffee.wms.return_request.application.port.`in`.StartReturnInspectingUseCase
import com.dozycoffee.wms.return_request.domain.enumeration.ReturnRequestStatus
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
@RequestMapping("/api/return-requests")
class ReturnRequestController(
    private val registerReturnRequestUseCase: RegisterReturnRequestUseCase,
    private val startReturnInspectingUseCase: StartReturnInspectingUseCase,
    private val completeReturnRequestUseCase: CompleteReturnRequestUseCase,
    private val getReturnRequestUseCase: GetReturnRequestUseCase
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun register(@Valid @RequestBody request: RegisterReturnRequestRequest): ReturnRequestResponse {
        return ReturnRequestResponse.from(registerReturnRequestUseCase.register(request.toCommand()))
    }

    @GetMapping("/{returnRequestId}")
    suspend fun getById(@PathVariable returnRequestId: Long): ReturnRequestResponse {
        return ReturnRequestResponse.from(getReturnRequestUseCase.getById(returnRequestId))
    }

    @GetMapping
    fun getAll(@RequestParam(required = false) status: ReturnRequestStatus?): Flow<ReturnRequestResponse> {
        return getReturnRequestUseCase.getAll(status).map { ReturnRequestResponse.from(it) }
    }

    @PatchMapping("/{returnRequestId}/start-inspecting")
    suspend fun startInspecting(@PathVariable returnRequestId: Long): ReturnRequestResponse {
        return ReturnRequestResponse.from(startReturnInspectingUseCase.startInspecting(returnRequestId))
    }

    @PatchMapping("/{returnRequestId}/complete")
    suspend fun complete(
        @PathVariable returnRequestId: Long,
        @Valid @RequestBody request: CompleteReturnRequestRequest
    ): ReturnRequestResponse {
        return ReturnRequestResponse.from(completeReturnRequestUseCase.complete(request.toCommand(returnRequestId)))
    }
}
