package com.dozycoffee.wms.warehouse.adapter.in.web;

import com.dozycoffee.wms.warehouse.adapter.in.web.request.RegisterZoneRequest;
import com.dozycoffee.wms.warehouse.adapter.in.web.response.ZoneResponse;
import com.dozycoffee.wms.warehouse.application.port.in.GetZoneUseCase;
import com.dozycoffee.wms.warehouse.application.port.in.RegisterZoneUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class ZoneController {

    private final RegisterZoneUseCase registerZoneUseCase;
    private final GetZoneUseCase getZoneUseCase;

    @PostMapping("/api/warehouses/{warehouseId}/zones")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ZoneResponse> register(@PathVariable Long warehouseId, @Valid @RequestBody RegisterZoneRequest request) {
        return registerZoneUseCase.register(request.toCommand(warehouseId))
                .map(ZoneResponse::from);
    }

    @GetMapping("/api/zones/{zoneId}")
    public Mono<ZoneResponse> getById(@PathVariable Long zoneId) {
        return getZoneUseCase.getById(zoneId)
                .map(ZoneResponse::from);
    }
}
