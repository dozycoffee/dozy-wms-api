package com.dozycoffee.wms.warehouse.domain.exception;

import com.dozycoffee.wms.global.error.DomainException;

public class InactiveLocationException extends DomainException {
    public InactiveLocationException() {
        super(LocationErrorCode.INACTIVE_LOCATION);
    }
}
