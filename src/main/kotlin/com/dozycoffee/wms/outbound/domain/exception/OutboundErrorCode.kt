package com.dozycoffee.wms.outbound.domain.exception

import com.dozycoffee.wms.global.error.ErrorCode
import com.dozycoffee.wms.global.error.ErrorType

enum class OutboundErrorCode(
    override val errorType: ErrorType,
    override val code: String,
    override val message: String
) : ErrorCode {

    INVALID_WAREHOUSE_ID(ErrorType.VALIDATION, "OUTBOUND_INVALID_WAREHOUSE_ID", "창고는 필수입니다."),
    INVALID_STATUS_TRANSITION(
        ErrorType.CONFLICT,
        "OUTBOUND_INVALID_STATUS_TRANSITION",
        "출고 상태를 역행하거나 건너뛸 수 없습니다."
    ),
    OUTBOUND_NOT_FOUND(ErrorType.NOT_FOUND, "OUTBOUND_NOT_FOUND", "존재하지 않는 출고입니다.")
}
