package com.dozycoffee.wms.warehouse.domain.exception

import com.dozycoffee.wms.global.error.ErrorCode
import com.dozycoffee.wms.global.error.ErrorType

enum class LocationErrorCode(
    override val errorType: ErrorType,
    override val code: String,
    override val message: String
) : ErrorCode {

    INVALID_ZONE_ID(ErrorType.VALIDATION, "LOCATION_INVALID_ZONE_ID", "소속 구역은 필수입니다."),
    INVALID_LOCATION_CODE(ErrorType.VALIDATION, "LOCATION_INVALID_LOCATION_CODE", "위치 코드는 'A-01' 형식이어야 합니다."),
    INVALID_LOCATION_STATUS(ErrorType.VALIDATION, "LOCATION_INVALID_LOCATION_STATUS", "위치 상태는 필수입니다."),
    INVALID_AMOUNT(ErrorType.VALIDATION, "LOCATION_INVALID_AMOUNT", "처리 수량은 0보다 커야 합니다."),
    CAPACITY_EXCEEDED(ErrorType.CONFLICT, "LOCATION_CAPACITY_EXCEEDED", "위치의 최대 수용량을 초과할 수 없습니다."),
    INSUFFICIENT_USED_CAPACITY(ErrorType.CONFLICT, "LOCATION_INSUFFICIENT_USED_CAPACITY", "반출 수량이 현재 사용량보다 많습니다."),
    INACTIVE_LOCATION(ErrorType.CONFLICT, "LOCATION_INACTIVE", "비활성화된 위치는 적재/반출할 수 없습니다."),
    LOCATION_NOT_FOUND(ErrorType.NOT_FOUND, "LOCATION_NOT_FOUND", "존재하지 않는 위치입니다.")
}
