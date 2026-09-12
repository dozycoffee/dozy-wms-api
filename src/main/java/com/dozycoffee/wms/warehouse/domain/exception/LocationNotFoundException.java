package com.dozycoffee.wms.warehouse.domain.exception;

import com.dozycoffee.wms.global.error.ApplicationException;

public class LocationNotFoundException extends ApplicationException {
    public LocationNotFoundException() {
        super(LocationErrorCode.LOCATION_NOT_FOUND);
    }
}
