package com.dozycoffee.wms.warehouse.domain.exception;

import com.dozycoffee.wms.global.error.ApplicationException;

public class WorkAreaNotFoundException extends ApplicationException {
    public WorkAreaNotFoundException() {
        super(WorkAreaErrorCode.WORK_AREA_NOT_FOUND);
    }
}
