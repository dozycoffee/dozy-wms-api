package com.dozycoffee.wms.disposal.adapter.`in`.web

import com.dozycoffee.wms.disposal.adapter.`in`.web.request.RegisterDisposalRequest
import com.dozycoffee.wms.disposal.adapter.`in`.web.response.DisposalResponse
import com.dozycoffee.wms.disposal.application.port.`in`.ApproveDisposalUseCase
import com.dozycoffee.wms.disposal.application.port.`in`.CompleteDisposalUseCase
import com.dozycoffee.wms.disposal.application.port.`in`.GetDisposalUseCase
import com.dozycoffee.wms.disposal.application.port.`in`.RegisterDisposalUseCase
import com.dozycoffee.wms.disposal.domain.enumeration.DisposalStatus
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
@RequestMapping("/api/disposals")
class DisposalController(
    private val registerDisposalUseCase: RegisterDisposalUseCase,
    private val approveDisposalUseCase: ApproveDisposalUseCase,
    private val completeDisposalUseCase: CompleteDisposalUseCase,
    private val getDisposalUseCase: GetDisposalUseCase
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun register(@Valid @RequestBody request: RegisterDisposalRequest): DisposalResponse {
        return DisposalResponse.from(registerDisposalUseCase.register(request.toCommand()))
    }

    @GetMapping("/{disposalId}")
    suspend fun getById(@PathVariable disposalId: Long): DisposalResponse {
        return DisposalResponse.from(getDisposalUseCase.getById(disposalId))
    }

    @GetMapping
    fun getAll(@RequestParam(required = false) status: DisposalStatus?): Flow<DisposalResponse> {
        return getDisposalUseCase.getAll(status).map { DisposalResponse.from(it) }
    }

    @PatchMapping("/{disposalId}/approve")
    suspend fun approve(@PathVariable disposalId: Long): DisposalResponse {
        return DisposalResponse.from(approveDisposalUseCase.approve(disposalId))
    }

    @PatchMapping("/{disposalId}/complete")
    suspend fun complete(@PathVariable disposalId: Long): DisposalResponse {
        return DisposalResponse.from(completeDisposalUseCase.complete(disposalId))
    }
}
