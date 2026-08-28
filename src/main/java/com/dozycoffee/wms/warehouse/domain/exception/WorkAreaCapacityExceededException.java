package com.dozycoffee.wms.warehouse.domain.exception;

import com.dozycoffee.wms.global.error.DomainException;

public class WorkAreaCapacityExceededException extends DomainException {
    public WorkAreaCapacityExceededException() {
        super(WorkAreaErrorCode.CAPACITY_EXCEEDED);
    }
}
