package com.dozycoffee.wms.global.error;

public final class DomainValidator {

    private DomainValidator() {
    }

    public static <T> T requireNonNull(T value, ErrorCode errorCode) {
        if (value == null) {
            throw new InvalidDomainValueException(errorCode);
        }
        return value;
    }
}
