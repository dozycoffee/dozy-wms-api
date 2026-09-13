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
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.exception.WorkAreaNotFoundException
import com.dozycoffee.wms.warehouse.domain.model.WorkArea
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono

@Service
class WorkAreaService(
    private val workAreaRepository: WorkAreaRepository
) : RegisterWorkAreaUseCase, OccupyWorkAreaUseCase, ReleaseWorkAreaUseCase, GetWorkAreaUseCase {

    @Transactional
    override fun register(command: RegisterWorkAreaCommand): Mono<WorkAreaResult> {
        val workArea = WorkArea.create(command.warehouseId, command.areaCode, AvailabilityStatus.AVAILABLE)
        return workAreaRepository.save(workArea).map { WorkAreaResult.from(it) }
    }

    @Transactional
    override fun occupy(command: OccupyWorkAreaCommand): Mono<WorkAreaResult> {
        return findWorkAreaOrThrow(command.workAreaId)
            .doOnNext { it.occupy(command.amount) }
            .flatMap { workAreaRepository.save(it) }
            .map { WorkAreaResult.from(it) }
    }

    @Transactional
    override fun release(command: ReleaseWorkAreaCommand): Mono<WorkAreaResult> {
        return findWorkAreaOrThrow(command.workAreaId)
            .doOnNext { it.release(command.amount) }
            .flatMap { workAreaRepository.save(it) }
            .map { WorkAreaResult.from(it) }
    }

    @Transactional(readOnly = true)
    override fun getById(workAreaId: Long): Mono<WorkAreaResult> {
        return findWorkAreaOrThrow(workAreaId).map { WorkAreaResult.from(it) }
    }

    private fun findWorkAreaOrThrow(workAreaId: Long): Mono<WorkArea> {
        return workAreaRepository.findById(workAreaId)
            .switchIfEmpty(Mono.error(WorkAreaNotFoundException()))
    }
}
