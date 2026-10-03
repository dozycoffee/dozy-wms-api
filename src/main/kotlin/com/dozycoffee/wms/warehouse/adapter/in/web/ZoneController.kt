package com.dozycoffee.wms.warehouse.adapter.`in`.web

import com.dozycoffee.wms.global.security.WmsAuthorize
import com.dozycoffee.wms.warehouse.adapter.`in`.web.request.RegisterZoneRequest
import com.dozycoffee.wms.warehouse.adapter.`in`.web.response.ZoneResponse
import com.dozycoffee.wms.warehouse.application.port.`in`.GetZoneUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.RegisterZoneUseCase
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class ZoneController(
    private val registerZoneUseCase: RegisterZoneUseCase,
    private val getZoneUseCase: GetZoneUseCase
) {

    @PostMapping("/api/warehouses/{warehouseId}/zones")
    @PreAuthorize(WmsAuthorize.ADMIN)
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun register(@PathVariable warehouseId: Long, @Valid @RequestBody request: RegisterZoneRequest): ZoneResponse {
        return ZoneResponse.from(registerZoneUseCase.register(request.toCommand(warehouseId)))
    }

    @GetMapping("/api/zones/{zoneId}")
    @PreAuthorize(WmsAuthorize.READ)
    suspend fun getById(@PathVariable zoneId: Long): ZoneResponse {
        return ZoneResponse.from(getZoneUseCase.getById(zoneId))
    }

    @GetMapping("/api/warehouses/{warehouseId}/zones/{zoneCode}")
    @PreAuthorize(WmsAuthorize.READ)
    suspend fun getByWarehouseIdAndZoneCode(
        @PathVariable warehouseId: Long,
        @PathVariable zoneCode: ZoneCode
    ): ZoneResponse {
        return ZoneResponse.from(getZoneUseCase.getByWarehouseIdAndZoneCode(warehouseId, zoneCode))
    }
}
