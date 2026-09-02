package com.dozycoffee.wms.warehouse.domain.repository;

import com.dozycoffee.wms.warehouse.domain.model.Zone;

import java.util.Optional;

public interface ZoneRepository {

    Zone save(Zone zone);

    Optional<Zone> findById(Long zoneId);

    void delete(Zone zone);
}
