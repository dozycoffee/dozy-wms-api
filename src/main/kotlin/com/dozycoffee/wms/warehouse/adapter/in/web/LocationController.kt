package com.dozycoffee.wms.warehouse.adapter.`in`.web

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
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class LocationController(
    private val registerLocationUseCase: RegisterLocationUseCase,
    private val occupyLocationUseCase: OccupyLocationUseCase,
    private val releaseLocationUseCase: ReleaseLocationUseCase,
    private val getLocationUseCase: GetLocationUseCase
) {

    @PostMapping("/api/zones/{zoneId}/locations")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@PathVariable zoneId: Long, @Valid @RequestBody request: RegisterLocationRequest): Mono<LocationResponse> {
        return registerLocationUseCase.register(request.toCommand(zoneId)).map { LocationResponse.from(it) }
    }

    @GetMapping("/api/locations/{locationId}")
    fun getById(@PathVariable locationId: Long): Mono<LocationResponse> {
        return getLocationUseCase.getById(locationId).map { LocationResponse.from(it) }
    }

    @PatchMapping("/api/locations/{locationId}/occupy")
    fun occupy(@PathVariable locationId: Long, @Valid @RequestBody request: AmountRequest): Mono<LocationResponse> {
        return occupyLocationUseCase.occupy(OccupyLocationCommand(locationId, request.amount))
            .map { LocationResponse.from(it) }
    }

    @PatchMapping("/api/locations/{locationId}/release")
    fun release(@PathVariable locationId: Long, @Valid @RequestBody request: AmountRequest): Mono<LocationResponse> {
        return releaseLocationUseCase.release(ReleaseLocationCommand(locationId, request.amount))
            .map { LocationResponse.from(it) }
    }
}
