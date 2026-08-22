package com.dozycoffee.wms.warehouse.domain.repository;

import com.dozycoffee.wms.warehouse.domain.model.Warehouse;

import java.util.Optional;

public interface WarehouseRepository {

    Warehouse save(Warehouse warehouse);

    Optional<Warehouse> findById(Long warehouseId);

    void delete(Warehouse warehouse);
}
