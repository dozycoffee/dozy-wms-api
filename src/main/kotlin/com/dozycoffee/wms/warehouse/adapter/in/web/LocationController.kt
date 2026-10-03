package com.dozycoffee.wms.warehouse.adapter.`in`.web

import com.dozycoffee.wms.global.security.WmsAuthorize
import com.dozycoffee.wms.warehouse.adapter.`in`.web.request.AmountRequest
import com.dozycoffee.wms.warehouse.adapter.`in`.web.request.RegisterLocationRequest
import com.dozycoffee.wms.warehouse.adapter.`in`.web.response.LocationResponse
import com.dozycoffee.wms.warehouse.application.port.`in`.GetLocationUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.OccupyLocationUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.RegisterLocationUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.ReleaseLocationUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.command.OccupyLocationCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.command.ReleaseLocationCommand
import jakarta.validation.Valid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class LocationController(
    private val registerLocationUseCase: RegisterLocationUseCase,
    private val occupyLocationUseCase: OccupyLocationUseCase,
    private val releaseLocationUseCase: ReleaseLocationUseCase,
    private val getLocationUseCase: GetLocationUseCase
) {

    @PostMapping("/api/zones/{zoneId}/locations")
    @PreAuthorize(WmsAuthorize.ADMIN)
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun register(@PathVariable zoneId: Long, @Valid @RequestBody request: RegisterLocationRequest): LocationResponse {
        return LocationResponse.from(registerLocationUseCase.register(request.toCommand(zoneId)))
    }

    @GetMapping("/api/locations/{locationId}")
    @PreAuthorize(WmsAuthorize.READ)
    suspend fun getById(@PathVariable locationId: Long): LocationResponse {
        return LocationResponse.from(getLocationUseCase.getById(locationId))
    }

    @GetMapping("/api/zones/{zoneId}/locations")
    @PreAuthorize(WmsAuthorize.READ)
    fun getByZoneId(@PathVariable zoneId: Long): Flow<LocationResponse> {
        return getLocationUseCase.getByZoneId(zoneId).map { LocationResponse.from(it) }
    }

    @PatchMapping("/api/locations/{locationId}/occupy")
    @PreAuthorize(WmsAuthorize.ADMIN)
    suspend fun occupy(@PathVariable locationId: Long, @Valid @RequestBody request: AmountRequest): LocationResponse {
        return LocationResponse.from(occupyLocationUseCase.occupy(OccupyLocationCommand(locationId, request.amount)))
    }

    @PatchMapping("/api/locations/{locationId}/release")
    @PreAuthorize(WmsAuthorize.ADMIN)
    suspend fun release(@PathVariable locationId: Long, @Valid @RequestBody request: AmountRequest): LocationResponse {
        return LocationResponse.from(releaseLocationUseCase.release(ReleaseLocationCommand(locationId, request.amount)))
    }
}
