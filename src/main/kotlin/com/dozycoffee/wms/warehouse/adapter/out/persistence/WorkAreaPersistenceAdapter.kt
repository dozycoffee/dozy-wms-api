package com.dozycoffee.wms.warehouse.adapter.out.persistence

import com.dozycoffee.wms.global.persistence.CommonCodes
import com.dozycoffee.wms.warehouse.application.port.out.WorkAreaRepository
import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode
import com.dozycoffee.wms.warehouse.domain.model.WorkArea
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class WorkAreaPersistenceAdapter(
    private val workAreaR2dbcRepository: WorkAreaR2dbcRepository
) : WorkAreaRepository {

    companion object {
        private const val AREA_CODE_GROUP = "WORK_AREA_TYPE"
    }

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

    override fun findByWarehouseIdAndAreaCode(warehouseId: Long, areaCode: AreaCode): Mono<WorkArea> {
        val code = CommonCodes.toCode(AREA_CODE_GROUP, areaCode)
        return workAreaR2dbcRepository.findByWarehouseIdAndAreaCode(warehouseId, code).map { it.toDomain() }
    }

    override fun delete(workArea: WorkArea): Mono<Void> {
        return workAreaR2dbcRepository.delete(WorkAreaEntity.from(workArea))
    }
}
