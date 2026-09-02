package com.dozycoffee.wms.warehouse.domain.exception;

import com.dozycoffee.wms.global.error.DomainException;

public class InsufficientLocationCapacityException extends DomainException {
    public InsufficientLocationCapacityException() {
        super(LocationErrorCode.INSUFFICIENT_USED_CAPACITY);
    }
}
