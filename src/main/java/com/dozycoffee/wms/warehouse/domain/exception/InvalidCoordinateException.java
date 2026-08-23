package com.dozycoffee.wms.warehouse.domain.exception;

import com.dozycoffee.wms.global.error.DomainException;

public class InvalidCoordinateException extends DomainException {
    public InvalidCoordinateException() {
        super(WarehouseErrorCode.INVALID_COORDINATE);
    }
}
