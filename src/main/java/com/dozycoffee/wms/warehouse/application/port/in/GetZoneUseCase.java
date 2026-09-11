package com.dozycoffee.wms.warehouse.application.port.in;

import reactor.core.publisher.Mono;

public interface GetZoneUseCase {

    Mono<ZoneResult> getById(Long zoneId);
}
