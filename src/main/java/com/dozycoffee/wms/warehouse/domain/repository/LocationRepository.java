package com.dozycoffee.wms.warehouse.domain.repository;

import com.dozycoffee.wms.warehouse.domain.model.Location;

import java.util.Optional;

public interface LocationRepository {

    Location save(Location location);

    Optional<Location> findById(Long locationId);

    void delete(Location location);
}
