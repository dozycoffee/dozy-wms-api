package com.dozycoffee.wms.warehouse.application.port.in;

import com.dozycoffee.wms.warehouse.application.port.in.command.RegisterWarehouseCommand;
import com.dozycoffee.wms.warehouse.application.port.in.result.WarehouseResult;
import reactor.core.publisher.Mono;

public interface RegisterWarehouseUseCase {

    Mono<WarehouseResult> register(RegisterWarehouseCommand command);
}
