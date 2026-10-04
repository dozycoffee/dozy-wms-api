package com.dozycoffee.wms.warehouse.application.service

import com.dozycoffee.wms.warehouse.application.port.`in`.GetWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.OccupyWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.RegisterWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.ReleaseWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.command.OccupyWorkAreaCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.command.RegisterWorkAreaCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.command.ReleaseWorkAreaCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.result.WorkAreaResult
import com.dozycoffee.wms.warehouse.application.port.out.WorkAreaRepository
import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.exception.WorkAreaNotFoundException
import com.dozycoffee.wms.warehouse.domain.model.WorkArea
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class WorkAreaService(
    private val workAreaRepository: WorkAreaRepository
) : RegisterWorkAreaUseCase, OccupyWorkAreaUseCase, ReleaseWorkAreaUseCase, GetWorkAreaUseCase {

    @Transactional
    override suspend fun register(command: RegisterWorkAreaCommand): WorkAreaResult {
        val workArea = WorkArea.create(command.warehouseId, command.areaCode, AvailabilityStatus.AVAILABLE)
        return WorkAreaResult.from(workAreaRepository.save(workArea))
    }

    @Transactional
    override suspend fun occupy(command: OccupyWorkAreaCommand): WorkAreaResult {
        val workArea = findWorkAreaOrThrow(command.workAreaId)
        workArea.occupy(command.amount)
        return WorkAreaResult.from(workAreaRepository.save(workArea))
    }

    @Transactional
    override suspend fun release(command: ReleaseWorkAreaCommand): WorkAreaResult {
        val workArea = findWorkAreaOrThrow(command.workAreaId)
        workArea.release(command.amount)
        return WorkAreaResult.from(workAreaRepository.save(workArea))
    }

    @Transactional(readOnly = true)
    override suspend fun getById(workAreaId: Long): WorkAreaResult {
        return WorkAreaResult.from(findWorkAreaOrThrow(workAreaId))
    }

    @Transactional(readOnly = true)
    override suspend fun getByWarehouseIdAndAreaCode(warehouseId: Long, areaCode: AreaCode): WorkAreaResult {
        val workArea = workAreaRepository.findByWarehouseIdAndAreaCode(warehouseId, areaCode)
            ?: throw WorkAreaNotFoundException()
        return WorkAreaResult.from(workArea)
    }

    private suspend fun findWorkAreaOrThrow(workAreaId: Long): WorkArea {
        return workAreaRepository.findById(workAreaId) ?: throw WorkAreaNotFoundException()
    }
}
