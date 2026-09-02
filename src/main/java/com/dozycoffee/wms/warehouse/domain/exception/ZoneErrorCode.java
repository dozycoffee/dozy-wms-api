package com.dozycoffee.wms.warehouse.domain.exception;

import com.dozycoffee.wms.global.error.ErrorCode;
import com.dozycoffee.wms.global.error.ErrorType;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ZoneErrorCode implements ErrorCode {

    INVALID_WAREHOUSE_ID(ErrorType.VALIDATION, "ZONE_INVALID_WAREHOUSE_ID", "소속 창고는 필수입니다."),
    INVALID_ZONE_CODE(ErrorType.VALIDATION, "ZONE_INVALID_ZONE_CODE", "구역 코드는 필수입니다."),
    INVALID_ZONE_STATUS(ErrorType.VALIDATION, "ZONE_INVALID_ZONE_STATUS", "구역 상태는 필수입니다.");

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
