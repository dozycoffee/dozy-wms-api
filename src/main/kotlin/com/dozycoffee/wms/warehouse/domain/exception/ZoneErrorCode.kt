package com.dozycoffee.wms.warehouse.domain.exception

import com.dozycoffee.wms.global.error.ErrorCode
import com.dozycoffee.wms.global.error.ErrorType

enum class ZoneErrorCode(
    override val errorType: ErrorType,
    override val code: String,
    override val message: String
) : ErrorCode {

    INVALID_WAREHOUSE_ID(ErrorType.VALIDATION, "ZONE_INVALID_WAREHOUSE_ID", "소속 창고는 필수입니다."),
    INVALID_ZONE_CODE(ErrorType.VALIDATION, "ZONE_INVALID_ZONE_CODE", "구역 코드는 필수입니다."),
    INVALID_ZONE_STATUS(ErrorType.VALIDATION, "ZONE_INVALID_ZONE_STATUS", "구역 상태는 필수입니다."),
    ZONE_NOT_FOUND(ErrorType.NOT_FOUND, "ZONE_NOT_FOUND", "존재하지 않는 구역입니다.")
}
