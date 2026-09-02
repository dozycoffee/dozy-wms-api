package com.dozycoffee.wms.warehouse.domain.repository;

import com.dozycoffee.wms.warehouse.domain.model.WorkArea;

import java.util.Optional;

public interface WorkAreaRepository {

    WorkArea save(WorkArea workArea);

    Optional<WorkArea> findById(Long workAreaId);

    void delete(WorkArea workArea);
}
