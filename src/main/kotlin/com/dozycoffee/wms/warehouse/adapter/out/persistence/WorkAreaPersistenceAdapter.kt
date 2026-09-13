package com.dozycoffee.wms.warehouse.adapter.out.persistence

import com.dozycoffee.wms.warehouse.application.port.out.WorkAreaRepository
import com.dozycoffee.wms.warehouse.domain.model.WorkArea
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class WorkAreaPersistenceAdapter(
    private val workAreaR2dbcRepository: WorkAreaR2dbcRepository
) : WorkAreaRepository {

    override fun save(workArea: WorkArea): Mono<WorkArea> {
        val entity = WorkAreaEntity.from(workArea)
        val workAreaId = workArea.workAreaId
            ?: return workAreaR2dbcRepository.save(entity).map { it.toDomain() }
        return workAreaR2dbcRepository.findById(workAreaId)
            .doOnNext { entity.copyAuditFieldsFrom(it) }
            .then(workAreaR2dbcRepository.save(entity))
            .map { it.toDomain() }
    }

    override fun findById(workAreaId: Long): Mono<WorkArea> {
        return workAreaR2dbcRepository.findById(workAreaId).map { it.toDomain() }
    }

    override fun delete(workArea: WorkArea): Mono<Void> {
        return workAreaR2dbcRepository.delete(WorkAreaEntity.from(workArea))
    }
}
