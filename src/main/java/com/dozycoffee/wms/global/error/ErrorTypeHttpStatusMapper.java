package com.dozycoffee.wms.global.error;

import org.springframework.http.HttpStatus;

public final class ErrorTypeHttpStatusMapper {

    private ErrorTypeHttpStatusMapper() {
    }

    public static HttpStatus resolve(ErrorType errorType) {
        return switch (errorType) {
            case VALIDATION -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case INTERNAL -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
