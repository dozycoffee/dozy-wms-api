package com.dozycoffee.wms.warehouse.application.port.in;

import com.dozycoffee.wms.warehouse.application.port.in.command.RegisterLocationCommand;
import com.dozycoffee.wms.warehouse.application.port.in.result.LocationResult;
import reactor.core.publisher.Mono;

public interface RegisterLocationUseCase {

    Mono<LocationResult> register(RegisterLocationCommand command);
}
