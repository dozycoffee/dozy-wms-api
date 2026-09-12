package com.dozycoffee.wms.warehouse.application.port.in;

import com.dozycoffee.wms.warehouse.application.port.in.result.ZoneResult;
import reactor.core.publisher.Mono;

public interface GetZoneUseCase {

    Mono<ZoneResult> getById(Long zoneId);
}
