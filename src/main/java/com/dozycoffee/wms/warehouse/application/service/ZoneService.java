package com.dozycoffee.wms.warehouse.application.service;

import com.dozycoffee.wms.warehouse.application.port.in.GetZoneUseCase;
import com.dozycoffee.wms.warehouse.application.port.in.RegisterZoneUseCase;
import com.dozycoffee.wms.warehouse.application.port.in.command.RegisterZoneCommand;
import com.dozycoffee.wms.warehouse.application.port.in.result.ZoneResult;
import com.dozycoffee.wms.warehouse.application.port.out.ZoneRepository;
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus;
import com.dozycoffee.wms.warehouse.domain.exception.ZoneNotFoundException;
import com.dozycoffee.wms.warehouse.domain.model.Zone;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ZoneService implements RegisterZoneUseCase, GetZoneUseCase {

    private final ZoneRepository zoneRepository;

    @Override
    @Transactional
    public Mono<ZoneResult> register(RegisterZoneCommand command) {
        Zone zone = Zone.create(command.warehouseId(), command.zoneCode(), AvailabilityStatus.AVAILABLE);
        return zoneRepository.save(zone)
                .map(ZoneResult::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<ZoneResult> getById(Long zoneId) {
        return zoneRepository.findById(zoneId)
                .switchIfEmpty(Mono.error(new ZoneNotFoundException()))
                .map(ZoneResult::from);
    }
}
