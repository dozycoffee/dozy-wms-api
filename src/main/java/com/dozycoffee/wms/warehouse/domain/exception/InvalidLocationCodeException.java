package com.dozycoffee.wms.warehouse.domain.exception;

import com.dozycoffee.wms.global.error.DomainException;

public class InvalidLocationCodeException extends DomainException {
    public InvalidLocationCodeException() {
        super(LocationErrorCode.INVALID_LOCATION_CODE);
    }
}
