package com.dozycoffee.wms.global.error;

import java.time.LocalDateTime;

public record ErrorResponseDto(String errorCode, String message, LocalDateTime timestamp) {

    public static ErrorResponseDto of(ErrorCode errorCode) {
        return new ErrorResponseDto(errorCode.getCode(), errorCode.getMessage(), LocalDateTime.now());
    }
}
