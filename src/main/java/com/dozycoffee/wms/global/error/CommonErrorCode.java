package com.dozycoffee.wms.global.error;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    INVALID_INPUT(ErrorType.VALIDATION, "COMMON_INVALID_INPUT", "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(ErrorType.INTERNAL, "COMMON_INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.");

    private final ErrorType errorType;
    private final String code;
    private final String message;

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public ErrorType getErrorType() {
        return errorType;
    }
}
