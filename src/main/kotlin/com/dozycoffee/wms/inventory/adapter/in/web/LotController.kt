package com.dozycoffee.wms.inventory.adapter.`in`.web

import com.dozycoffee.wms.inventory.adapter.`in`.web.request.RegisterLotRequest
import com.dozycoffee.wms.inventory.adapter.`in`.web.response.LotDetailResponse
import com.dozycoffee.wms.inventory.adapter.`in`.web.response.LotResponse
import com.dozycoffee.wms.inventory.application.port.`in`.GetLotUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.RegisterLotUseCase
import jakarta.validation.Valid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/lots")
class LotController(
    private val registerLotUseCase: RegisterLotUseCase,
    private val getLotUseCase: GetLotUseCase
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun register(@Valid @RequestBody request: RegisterLotRequest): LotResponse {
        return LotResponse.from(registerLotUseCase.register(request.toCommand()))
    }

    @GetMapping("/{lotId}")
    suspend fun getById(@PathVariable lotId: Long): LotDetailResponse {
        return LotDetailResponse.from(getLotUseCase.getDetailById(lotId))
    }

    @GetMapping
    fun getAllByProduct(@RequestParam productId: Long): Flow<LotResponse> {
        return getLotUseCase.getAllByProduct(productId).map { LotResponse.from(it) }
    }
}
