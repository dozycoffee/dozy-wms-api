package com.dozycoffee.wms.warehouse.adapter.`in`.web

import com.dozycoffee.wms.global.security.WmsAuthorize
import com.dozycoffee.wms.warehouse.adapter.`in`.web.request.RegisterWarehouseRequest
import com.dozycoffee.wms.warehouse.adapter.`in`.web.response.WarehouseResponse
import com.dozycoffee.wms.warehouse.application.port.`in`.ActivateWarehouseUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.DeactivateWarehouseUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.GetWarehouseUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.RegisterWarehouseUseCase
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/warehouses")
class WarehouseController(
    private val registerWarehouseUseCase: RegisterWarehouseUseCase,
    private val activateWarehouseUseCase: ActivateWarehouseUseCase,
    private val deactivateWarehouseUseCase: DeactivateWarehouseUseCase,
    private val getWarehouseUseCase: GetWarehouseUseCase
) {

    @PostMapping
    @PreAuthorize(WmsAuthorize.ADMIN)
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun register(@Valid @RequestBody request: RegisterWarehouseRequest): WarehouseResponse {
        return WarehouseResponse.from(registerWarehouseUseCase.register(request.toCommand()))
    }

    @GetMapping("/{warehouseId}")
    @PreAuthorize(WmsAuthorize.READ)
    suspend fun getById(@PathVariable warehouseId: Long): WarehouseResponse {
        return WarehouseResponse.from(getWarehouseUseCase.getById(warehouseId))
    }

    @PatchMapping("/{warehouseId}/activate")
    @PreAuthorize(WmsAuthorize.ADMIN)
    suspend fun activate(@PathVariable warehouseId: Long): WarehouseResponse {
        return WarehouseResponse.from(activateWarehouseUseCase.activate(warehouseId))
    }

    @PatchMapping("/{warehouseId}/deactivate")
    @PreAuthorize(WmsAuthorize.ADMIN)
    suspend fun deactivate(@PathVariable warehouseId: Long): WarehouseResponse {
        return WarehouseResponse.from(deactivateWarehouseUseCase.deactivate(warehouseId))
    }
}
