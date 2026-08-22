package com.dozycoffee.wms.warehouse.domain.exception;

import com.dozycoffee.wms.global.error.DomainException;

public class InvalidCapacityException extends DomainException {
    public InvalidCapacityException() {
        super(WarehouseErrorCode.INVALID_CAPACITY);
    }
}