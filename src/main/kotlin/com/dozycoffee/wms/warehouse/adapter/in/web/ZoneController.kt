package com.dozycoffee.wms.warehouse.adapter.`in`.web

import com.dozycoffee.wms.warehouse.adapter.`in`.web.request.RegisterZoneRequest
import com.dozycoffee.wms.warehouse.adapter.`in`.web.response.ZoneResponse
import com.dozycoffee.wms.warehouse.application.port.`in`.GetZoneUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.RegisterZoneUseCase
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class ZoneController(
    private val registerZoneUseCase: RegisterZoneUseCase,
    private val getZoneUseCase: GetZoneUseCase
) {

    @PostMapping("/api/warehouses/{warehouseId}/zones")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@PathVariable warehouseId: Long, @Valid @RequestBody request: RegisterZoneRequest): Mono<ZoneResponse> {
        return registerZoneUseCase.register(request.toCommand(warehouseId)).map { ZoneResponse.from(it) }
    }

    @GetMapping("/api/zones/{zoneId}")
    fun getById(@PathVariable zoneId: Long): Mono<ZoneResponse> {
        return getZoneUseCase.getById(zoneId).map { ZoneResponse.from(it) }
    }

    @GetMapping("/api/warehouses/{warehouseId}/zones/{zoneCode}")
    fun getByWarehouseIdAndZoneCode(
        @PathVariable warehouseId: Long,
        @PathVariable zoneCode: ZoneCode
    ): Mono<ZoneResponse> {
        return getZoneUseCase.getByWarehouseIdAndZoneCode(warehouseId, zoneCode).map { ZoneResponse.from(it) }
    }
}
