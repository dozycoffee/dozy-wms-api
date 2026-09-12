package com.dozycoffee.wms.warehouse.adapter.in.web;

import com.dozycoffee.wms.warehouse.adapter.in.web.request.AmountRequest;
import com.dozycoffee.wms.warehouse.adapter.in.web.request.RegisterWorkAreaRequest;
import com.dozycoffee.wms.warehouse.adapter.in.web.response.WorkAreaResponse;
import com.dozycoffee.wms.warehouse.application.port.in.GetWorkAreaUseCase;
import com.dozycoffee.wms.warehouse.application.port.in.OccupyWorkAreaUseCase;
import com.dozycoffee.wms.warehouse.application.port.in.RegisterWorkAreaUseCase;
import com.dozycoffee.wms.warehouse.application.port.in.ReleaseWorkAreaUseCase;
import com.dozycoffee.wms.warehouse.application.port.in.command.OccupyWorkAreaCommand;
import com.dozycoffee.wms.warehouse.application.port.in.command.ReleaseWorkAreaCommand;
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
public class WorkAreaController {

    private final RegisterWorkAreaUseCase registerWorkAreaUseCase;
    private final OccupyWorkAreaUseCase occupyWorkAreaUseCase;
    private final ReleaseWorkAreaUseCase releaseWorkAreaUseCase;
    private final GetWorkAreaUseCase getWorkAreaUseCase;

    @PostMapping("/api/warehouses/{warehouseId}/work-areas")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<WorkAreaResponse> register(@PathVariable Long warehouseId, @Valid @RequestBody RegisterWorkAreaRequest request) {
        return registerWorkAreaUseCase.register(request.toCommand(warehouseId))
                .map(WorkAreaResponse::from);
    }

    @GetMapping("/api/work-areas/{workAreaId}")
    public Mono<WorkAreaResponse> getById(@PathVariable Long workAreaId) {
        return getWorkAreaUseCase.getById(workAreaId)
                .map(WorkAreaResponse::from);
    }

    @PatchMapping("/api/work-areas/{workAreaId}/occupy")
    public Mono<WorkAreaResponse> occupy(@PathVariable Long workAreaId, @Valid @RequestBody AmountRequest request) {
        return occupyWorkAreaUseCase.occupy(new OccupyWorkAreaCommand(workAreaId, request.amount()))
                .map(WorkAreaResponse::from);
    }

    @PatchMapping("/api/work-areas/{workAreaId}/release")
    public Mono<WorkAreaResponse> release(@PathVariable Long workAreaId, @Valid @RequestBody AmountRequest request) {
        return releaseWorkAreaUseCase.release(new ReleaseWorkAreaCommand(workAreaId, request.amount()))
                .map(WorkAreaResponse::from);
    }
}
