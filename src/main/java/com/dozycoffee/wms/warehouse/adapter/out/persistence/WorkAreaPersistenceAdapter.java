package com.dozycoffee.wms.warehouse.adapter.out.persistence;

import com.dozycoffee.wms.warehouse.application.port.out.WorkAreaRepository;
import com.dozycoffee.wms.warehouse.domain.model.WorkArea;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class WorkAreaPersistenceAdapter implements WorkAreaRepository {

    private final WorkAreaR2dbcRepository workAreaR2dbcRepository;

    @Override
    public Mono<WorkArea> save(WorkArea workArea) {
        WorkAreaEntity entity = WorkAreaEntity.from(workArea);
        if (workArea.getWorkAreaId() == null) {
            return workAreaR2dbcRepository.save(entity)
                    .map(WorkAreaEntity::toDomain);
        }
        return workAreaR2dbcRepository.findById(workArea.getWorkAreaId())
                .doOnNext(entity::copyAuditFieldsFrom)
                .then(workAreaR2dbcRepository.save(entity))
                .map(WorkAreaEntity::toDomain);
    }

    @Override
    public Mono<WorkArea> findById(Long workAreaId) {
        return workAreaR2dbcRepository.findById(workAreaId)
                .map(WorkAreaEntity::toDomain);
    }

    @Override
    public Mono<Void> delete(WorkArea workArea) {
        return workAreaR2dbcRepository.delete(WorkAreaEntity.from(workArea));
    }
}
