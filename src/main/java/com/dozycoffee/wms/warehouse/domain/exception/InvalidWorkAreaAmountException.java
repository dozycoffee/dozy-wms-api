package com.dozycoffee.wms.warehouse.domain.exception;

import com.dozycoffee.wms.global.error.DomainException;

public class InvalidWorkAreaAmountException extends DomainException {
    public InvalidWorkAreaAmountException() {
        super(WorkAreaErrorCode.INVALID_AMOUNT);
    }
}
