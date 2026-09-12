package com.dozycoffee.wms.warehouse.domain.exception;

import com.dozycoffee.wms.global.error.ApplicationException;

public class ZoneNotFoundException extends ApplicationException {
    public ZoneNotFoundException() {
        super(ZoneErrorCode.ZONE_NOT_FOUND);
    }
}
