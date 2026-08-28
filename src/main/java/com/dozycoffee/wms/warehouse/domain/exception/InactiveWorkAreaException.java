package com.dozycoffee.wms.warehouse.domain.exception;

import com.dozycoffee.wms.global.error.DomainException;

public class InactiveWorkAreaException extends DomainException {
    public InactiveWorkAreaException() {
        super(WorkAreaErrorCode.INACTIVE_WORK_AREA);
    }
}
