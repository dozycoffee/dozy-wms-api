package com.dozycoffee.wms.global.error;

public class DomainException extends BusinessException {
    protected DomainException(ErrorCode errorCode) {
        super(errorCode);
    }
}
