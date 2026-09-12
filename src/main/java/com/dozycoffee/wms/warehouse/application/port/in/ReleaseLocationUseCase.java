package com.dozycoffee.wms.warehouse.application.port.in;

import com.dozycoffee.wms.warehouse.application.port.in.command.ReleaseLocationCommand;
import com.dozycoffee.wms.warehouse.application.port.in.result.LocationResult;
import reactor.core.publisher.Mono;

public interface ReleaseLocationUseCase {

    Mono<LocationResult> release(ReleaseLocationCommand command);
}
