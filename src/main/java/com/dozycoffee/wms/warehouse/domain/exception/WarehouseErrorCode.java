package com.dozycoffee.wms.warehouse.domain.exception;

import com.dozycoffee.wms.global.error.ErrorCode;
import com.dozycoffee.wms.global.error.ErrorType;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum WarehouseErrorCode implements ErrorCode {

    INVALID_CAPACITY(ErrorType.VALIDATION, "WAREHOUSE_INVALID_CAPACITY", "적재 용량이 유효하지 않습니다."),
    INVALID_COORDINATE(ErrorType.VALIDATION, "WAREHOUSE_INVALID_COORDINATE", "좌표가 유효하지 않습니다."),
    INVALID_WAREHOUSE_NAME(ErrorType.VALIDATION, "WAREHOUSE_INVALID_WAREHOUSE_NAME", "창고명은 필수입니다."),
    INVALID_ADDRESS(ErrorType.VALIDATION, "WAREHOUSE_INVALID_ADDRESS", "주소는 필수입니다."),
    INVALID_WAREHOUSE_STATUS(ErrorType.VALIDATION, "WAREHOUSE_INVALID_WAREHOUSE_STATUS", "창고 상태는 필수입니다."),
    WAREHOUSE_NOT_FOUND(ErrorType.NOT_FOUND, "WAREHOUSE_NOT_FOUND", "존재하지 않는 창고입니다.");

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
