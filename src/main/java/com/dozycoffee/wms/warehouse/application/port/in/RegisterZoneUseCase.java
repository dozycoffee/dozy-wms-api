package com.dozycoffee.wms.warehouse.application.port.in;

import com.dozycoffee.wms.warehouse.application.port.in.command.RegisterZoneCommand;
import com.dozycoffee.wms.warehouse.application.port.in.result.ZoneResult;
import reactor.core.publisher.Mono;

public interface RegisterZoneUseCase {

    Mono<ZoneResult> register(RegisterZoneCommand command);
}
