package com.dozycoffee.wms.warehouse.domain.exception;

import com.dozycoffee.wms.global.error.DomainException;

public class LocationCapacityExceededException extends DomainException {
    public LocationCapacityExceededException() {
        super(LocationErrorCode.CAPACITY_EXCEEDED);
    }
}
