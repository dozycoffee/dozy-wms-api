package com.dozycoffee.wms.global.error;

public class ApplicationException extends BusinessException {
    protected ApplicationException(ErrorCode errorCode) {
        super(errorCode);
    }
}