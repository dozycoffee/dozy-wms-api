package com.dozycoffee.wms.warehouse.adapter.in.web;

import com.dozycoffee.wms.warehouse.adapter.in.web.request.RegisterWarehouseRequest;
import com.dozycoffee.wms.warehouse.adapter.in.web.response.WarehouseResponse;
import com.dozycoffee.wms.warehouse.application.port.in.ActivateWarehouseUseCase;
import com.dozycoffee.wms.warehouse.application.port.in.DeactivateWarehouseUseCase;
import com.dozycoffee.wms.warehouse.application.port.in.GetWarehouseUseCase;
import com.dozycoffee.wms.warehouse.application.port.in.RegisterWarehouseUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final RegisterWarehouseUseCase registerWarehouseUseCase;
    private final ActivateWarehouseUseCase activateWarehouseUseCase;
    private final DeactivateWarehouseUseCase deactivateWarehouseUseCase;
    private final GetWarehouseUseCase getWarehouseUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<WarehouseResponse> register(@Valid @RequestBody RegisterWarehouseRequest request) {
        return registerWarehouseUseCase.register(request.toCommand())
                .map(WarehouseResponse::from);
    }

    @GetMapping("/{warehouseId}")
    public Mono<WarehouseResponse> getById(@PathVariable Long warehouseId) {
        return getWarehouseUseCase.getById(warehouseId)
                .map(WarehouseResponse::from);
    }

    @PatchMapping("/{warehouseId}/activate")
    public Mono<WarehouseResponse> activate(@PathVariable Long warehouseId) {
        return activateWarehouseUseCase.activate(warehouseId)
                .map(WarehouseResponse::from);
    }

    @PatchMapping("/{warehouseId}/deactivate")
    public Mono<WarehouseResponse> deactivate(@PathVariable Long warehouseId) {
        return deactivateWarehouseUseCase.deactivate(warehouseId)
                .map(WarehouseResponse::from);
    }
}
