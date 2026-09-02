package com.dozycoffee.wms.warehouse.domain.exception;

import com.dozycoffee.wms.global.error.DomainException;

public class InsufficientWorkAreaCapacityException extends DomainException {
    public InsufficientWorkAreaCapacityException() {
        super(WorkAreaErrorCode.INSUFFICIENT_USED_CAPACITY);
    }
}
