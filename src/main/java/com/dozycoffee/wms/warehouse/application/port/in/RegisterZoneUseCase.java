package com.dozycoffee.wms.warehouse.application.port.in;

import reactor.core.publisher.Mono;

public interface RegisterZoneUseCase {

    Mono<ZoneResult> register(RegisterZoneCommand command);
}
