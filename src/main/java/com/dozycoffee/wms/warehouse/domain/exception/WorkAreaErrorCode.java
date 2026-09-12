package com.dozycoffee.wms.warehouse.domain.exception;

import com.dozycoffee.wms.global.error.ErrorCode;
import com.dozycoffee.wms.global.error.ErrorType;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum WorkAreaErrorCode implements ErrorCode {

    INVALID_WAREHOUSE_ID(ErrorType.VALIDATION, "WORK_AREA_INVALID_WAREHOUSE_ID", "소속 창고는 필수입니다."),
    INVALID_AREA_CODE(ErrorType.VALIDATION, "WORK_AREA_INVALID_AREA_CODE", "작업 구역 코드는 필수입니다."),
    INVALID_WORK_AREA_STATUS(ErrorType.VALIDATION, "WORK_AREA_INVALID_WORK_AREA_STATUS", "작업 구역 상태는 필수입니다."),
    INVALID_AMOUNT(ErrorType.VALIDATION, "WORK_AREA_INVALID_AMOUNT", "처리 수량은 0보다 커야 합니다."),
    CAPACITY_EXCEEDED(ErrorType.CONFLICT, "WORK_AREA_CAPACITY_EXCEEDED", "작업 구역의 최대 수용량을 초과할 수 없습니다."),
    INSUFFICIENT_USED_CAPACITY(ErrorType.CONFLICT, "WORK_AREA_INSUFFICIENT_USED_CAPACITY", "반출 수량이 현재 사용량보다 많습니다."),
    INACTIVE_WORK_AREA(ErrorType.CONFLICT, "WORK_AREA_INACTIVE", "비활성화된 작업 구역은 점유/반출할 수 없습니다."),
    WORK_AREA_NOT_FOUND(ErrorType.NOT_FOUND, "WORK_AREA_NOT_FOUND", "존재하지 않는 작업 구역입니다.");

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
