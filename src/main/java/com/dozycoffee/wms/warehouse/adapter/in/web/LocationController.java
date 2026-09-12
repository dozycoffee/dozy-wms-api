package com.dozycoffee.wms.warehouse.adapter.in.web;

import com.dozycoffee.wms.warehouse.adapter.in.web.request.AmountRequest;
import com.dozycoffee.wms.warehouse.adapter.in.web.request.RegisterLocationRequest;
import com.dozycoffee.wms.warehouse.adapter.in.web.response.LocationResponse;
import com.dozycoffee.wms.warehouse.application.port.in.GetLocationUseCase;
import com.dozycoffee.wms.warehouse.application.port.in.OccupyLocationUseCase;
import com.dozycoffee.wms.warehouse.application.port.in.RegisterLocationUseCase;
import com.dozycoffee.wms.warehouse.application.port.in.ReleaseLocationUseCase;
import com.dozycoffee.wms.warehouse.application.port.in.command.OccupyLocationCommand;
import com.dozycoffee.wms.warehouse.application.port.in.command.ReleaseLocationCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class LocationController {

    private final RegisterLocationUseCase registerLocationUseCase;
    private final OccupyLocationUseCase occupyLocationUseCase;
    private final ReleaseLocationUseCase releaseLocationUseCase;
    private final GetLocationUseCase getLocationUseCase;

    @PostMapping("/api/zones/{zoneId}/locations")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<LocationResponse> register(@PathVariable Long zoneId, @Valid @RequestBody RegisterLocationRequest request) {
        return registerLocationUseCase.register(request.toCommand(zoneId))
                .map(LocationResponse::from);
    }

    @GetMapping("/api/locations/{locationId}")
    public Mono<LocationResponse> getById(@PathVariable Long locationId) {
        return getLocationUseCase.getById(locationId)
                .map(LocationResponse::from);
    }

    @PatchMapping("/api/locations/{locationId}/occupy")
    public Mono<LocationResponse> occupy(@PathVariable Long locationId, @Valid @RequestBody AmountRequest request) {
        return occupyLocationUseCase.occupy(new OccupyLocationCommand(locationId, request.amount()))
                .map(LocationResponse::from);
    }

    @PatchMapping("/api/locations/{locationId}/release")
    public Mono<LocationResponse> release(@PathVariable Long locationId, @Valid @RequestBody AmountRequest request) {
        return releaseLocationUseCase.release(new ReleaseLocationCommand(locationId, request.amount()))
                .map(LocationResponse::from);
    }
}
