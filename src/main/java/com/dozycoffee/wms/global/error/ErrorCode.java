package com.dozycoffee.wms.global.error;

public interface ErrorCode {
    String getCode();
    String getMessage();
    ErrorType getErrorType();
}
