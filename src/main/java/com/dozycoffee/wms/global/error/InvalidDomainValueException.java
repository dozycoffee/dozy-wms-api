package com.dozycoffee.wms.global.error;

public class InvalidDomainValueException extends DomainException {
    public InvalidDomainValueException(ErrorCode errorCode) {
        super(errorCode);
    }
}
