package com.dozycoffee.wms.warehouse.adapter.out.persistence;

import com.dozycoffee.wms.warehouse.application.port.out.WarehouseRepository;
import com.dozycoffee.wms.warehouse.domain.model.Warehouse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class WarehousePersistenceAdapter implements WarehouseRepository {

    private final WarehouseR2dbcRepository warehouseR2dbcRepository;

    @Override
    public Mono<Warehouse> save(Warehouse warehouse) {
        WarehouseEntity entity = WarehouseEntity.from(warehouse);
        if (warehouse.getWarehouseId() == null) {
            return warehouseR2dbcRepository.save(entity)
                    .map(WarehouseEntity::toDomain);
        }
        return warehouseR2dbcRepository.findById(warehouse.getWarehouseId())
                .doOnNext(entity::copyAuditFieldsFrom)
                .then(warehouseR2dbcRepository.save(entity))
                .map(WarehouseEntity::toDomain);
    }

    @Override
    public Mono<Warehouse> findById(Long warehouseId) {
        return warehouseR2dbcRepository.findById(warehouseId)
                .map(WarehouseEntity::toDomain);
    }

    @Override
    public Mono<Void> delete(Warehouse warehouse) {
        return warehouseR2dbcRepository.delete(WarehouseEntity.from(warehouse));
    }
}
