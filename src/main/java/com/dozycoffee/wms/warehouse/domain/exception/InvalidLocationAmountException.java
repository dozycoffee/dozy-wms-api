package com.dozycoffee.wms.warehouse.domain.exception;

import com.dozycoffee.wms.global.error.DomainException;

public class InvalidLocationAmountException extends DomainException {
    public InvalidLocationAmountException() {
        super(LocationErrorCode.INVALID_AMOUNT);
    }
}
