package com.dozycoffee.wms.warehouse.domain.exception;

import com.dozycoffee.wms.global.error.ApplicationException;

public class WarehouseNotFoundException extends ApplicationException {
    public WarehouseNotFoundException() {
        super(WarehouseErrorCode.WAREHOUSE_NOT_FOUND);
    }
}
