package com.dozycoffee.wms.warehouse.adapter.`in`.web

import com.dozycoffee.wms.warehouse.adapter.`in`.web.request.RegisterWarehouseRequest
import com.dozycoffee.wms.warehouse.adapter.`in`.web.response.WarehouseResponse
import com.dozycoffee.wms.warehouse.application.port.`in`.ActivateWarehouseUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.DeactivateWarehouseUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.GetWarehouseUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.RegisterWarehouseUseCase
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/warehouses")
class WarehouseController(
    private val registerWarehouseUseCase: RegisterWarehouseUseCase,
    private val activateWarehouseUseCase: ActivateWarehouseUseCase,
    private val deactivateWarehouseUseCase: DeactivateWarehouseUseCase,
    private val getWarehouseUseCase: GetWarehouseUseCase
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody request: RegisterWarehouseRequest): Mono<WarehouseResponse> {
        return registerWarehouseUseCase.register(request.toCommand()).map { WarehouseResponse.from(it) }
    }

    @GetMapping("/{warehouseId}")
    fun getById(@PathVariable warehouseId: Long): Mono<WarehouseResponse> {
        return getWarehouseUseCase.getById(warehouseId).map { WarehouseResponse.from(it) }
    }

    @PatchMapping("/{warehouseId}/activate")
    fun activate(@PathVariable warehouseId: Long): Mono<WarehouseResponse> {
        return activateWarehouseUseCase.activate(warehouseId).map { WarehouseResponse.from(it) }
    }

    @PatchMapping("/{warehouseId}/deactivate")
    fun deactivate(@PathVariable warehouseId: Long): Mono<WarehouseResponse> {
        return deactivateWarehouseUseCase.deactivate(warehouseId).map { WarehouseResponse.from(it) }
    }
}
