package com.dozycoffee.wms.warehouse.adapter.out.persistence

import com.dozycoffee.wms.warehouse.application.port.out.WorkAreaRepository
import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode
import com.dozycoffee.wms.warehouse.domain.model.WorkArea
import org.springframework.stereotype.Component

@Component
class WorkAreaPersistenceAdapter(
    private val workAreaR2dbcRepository: WorkAreaR2dbcRepository
) : WorkAreaRepository {

    override suspend fun save(workArea: WorkArea): WorkArea {
        val entity = WorkAreaEntity.from(workArea)
        val workAreaId = workArea.workAreaId
        if (workAreaId != null) {
            workAreaR2dbcRepository.findById(workAreaId)?.let { entity.copyAuditFieldsFrom(it) }
        }
        return workAreaR2dbcRepository.save(entity).toDomain()
    }

    override suspend fun findById(workAreaId: Long): WorkArea? {
        return workAreaR2dbcRepository.findById(workAreaId)?.toDomain()
    }

    override suspend fun findByWarehouseIdAndAreaCode(warehouseId: Long, areaCode: AreaCode): WorkArea? {
        val code = areaCode.name
        return workAreaR2dbcRepository.findByWarehouseIdAndAreaCode(warehouseId, code)?.toDomain()
    }

    override suspend fun delete(workArea: WorkArea) {
        workAreaR2dbcRepository.delete(WorkAreaEntity.from(workArea))
    }
}
