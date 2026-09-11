package com.dozycoffee.wms.warehouse.application.service;

import com.dozycoffee.wms.warehouse.application.port.in.ActivateWarehouseUseCase;
import com.dozycoffee.wms.warehouse.application.port.in.DeactivateWarehouseUseCase;
import com.dozycoffee.wms.warehouse.application.port.in.GetWarehouseUseCase;
import com.dozycoffee.wms.warehouse.application.port.in.RegisterWarehouseCommand;
import com.dozycoffee.wms.warehouse.application.port.in.RegisterWarehouseUseCase;
import com.dozycoffee.wms.warehouse.application.port.in.WarehouseResult;
import com.dozycoffee.wms.warehouse.application.port.out.WarehouseRepository;
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus;
import com.dozycoffee.wms.warehouse.domain.exception.WarehouseNotFoundException;
import com.dozycoffee.wms.warehouse.domain.model.Warehouse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class WarehouseService implements
        RegisterWarehouseUseCase,
        ActivateWarehouseUseCase,
        DeactivateWarehouseUseCase,
        GetWarehouseUseCase {

    private final WarehouseRepository warehouseRepository;

    @Override
    @Transactional
    public Mono<WarehouseResult> register(RegisterWarehouseCommand command) {
        Warehouse warehouse = Warehouse.create(
                command.warehouseName(),
                command.address(),
                command.latitude(),
                command.longitude(),
                AvailabilityStatus.AVAILABLE
        );
        return warehouseRepository.save(warehouse)
                .map(WarehouseResult::from);
    }

    @Override
    @Transactional
    public Mono<WarehouseResult> activate(Long warehouseId) {
        return findWarehouseOrThrow(warehouseId)
                .doOnNext(Warehouse::activate)
                .flatMap(warehouseRepository::save)
                .map(WarehouseResult::from);
    }

    @Override
    @Transactional
    public Mono<WarehouseResult> deactivate(Long warehouseId) {
        return findWarehouseOrThrow(warehouseId)
                .doOnNext(Warehouse::deactivate)
                .flatMap(warehouseRepository::save)
                .map(WarehouseResult::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<WarehouseResult> getById(Long warehouseId) {
        return findWarehouseOrThrow(warehouseId)
                .map(WarehouseResult::from);
    }

    private Mono<Warehouse> findWarehouseOrThrow(Long warehouseId) {
        return warehouseRepository.findById(warehouseId)
                .switchIfEmpty(Mono.error(new WarehouseNotFoundException()));
    }
}
