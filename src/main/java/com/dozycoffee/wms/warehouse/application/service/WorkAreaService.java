package com.dozycoffee.wms.warehouse.application.service;

import com.dozycoffee.wms.warehouse.application.port.in.GetWorkAreaUseCase;
import com.dozycoffee.wms.warehouse.application.port.in.OccupyWorkAreaUseCase;
import com.dozycoffee.wms.warehouse.application.port.in.RegisterWorkAreaUseCase;
import com.dozycoffee.wms.warehouse.application.port.in.ReleaseWorkAreaUseCase;
import com.dozycoffee.wms.warehouse.application.port.in.command.OccupyWorkAreaCommand;
import com.dozycoffee.wms.warehouse.application.port.in.command.RegisterWorkAreaCommand;
import com.dozycoffee.wms.warehouse.application.port.in.command.ReleaseWorkAreaCommand;
import com.dozycoffee.wms.warehouse.application.port.in.result.WorkAreaResult;
import com.dozycoffee.wms.warehouse.application.port.out.WorkAreaRepository;
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus;
import com.dozycoffee.wms.warehouse.domain.exception.WorkAreaNotFoundException;
import com.dozycoffee.wms.warehouse.domain.model.WorkArea;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class WorkAreaService implements
        RegisterWorkAreaUseCase,
        OccupyWorkAreaUseCase,
        ReleaseWorkAreaUseCase,
        GetWorkAreaUseCase {

    private final WorkAreaRepository workAreaRepository;

    @Override
    @Transactional
    public Mono<WorkAreaResult> register(RegisterWorkAreaCommand command) {
        WorkArea workArea = WorkArea.create(command.warehouseId(), command.areaCode(), AvailabilityStatus.AVAILABLE);
        return workAreaRepository.save(workArea)
                .map(WorkAreaResult::from);
    }

    @Override
    @Transactional
    public Mono<WorkAreaResult> occupy(OccupyWorkAreaCommand command) {
        return findWorkAreaOrThrow(command.workAreaId())
                .doOnNext(workArea -> workArea.occupy(command.amount()))
                .flatMap(workAreaRepository::save)
                .map(WorkAreaResult::from);
    }

    @Override
    @Transactional
    public Mono<WorkAreaResult> release(ReleaseWorkAreaCommand command) {
        return findWorkAreaOrThrow(command.workAreaId())
                .doOnNext(workArea -> workArea.release(command.amount()))
                .flatMap(workAreaRepository::save)
                .map(WorkAreaResult::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<WorkAreaResult> getById(Long workAreaId) {
        return findWorkAreaOrThrow(workAreaId)
                .map(WorkAreaResult::from);
    }

    private Mono<WorkArea> findWorkAreaOrThrow(Long workAreaId) {
        return workAreaRepository.findById(workAreaId)
                .switchIfEmpty(Mono.error(new WorkAreaNotFoundException()));
    }
}
