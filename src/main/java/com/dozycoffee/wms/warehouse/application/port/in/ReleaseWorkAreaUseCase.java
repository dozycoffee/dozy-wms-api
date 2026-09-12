package com.dozycoffee.wms.warehouse.application.port.in;

import com.dozycoffee.wms.warehouse.application.port.in.command.ReleaseWorkAreaCommand;
import com.dozycoffee.wms.warehouse.application.port.in.result.WorkAreaResult;
import reactor.core.publisher.Mono;

public interface ReleaseWorkAreaUseCase {

    Mono<WorkAreaResult> release(ReleaseWorkAreaCommand command);
}
