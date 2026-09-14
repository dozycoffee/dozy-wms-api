package com.dozycoffee.wms.inbound.domain.exception

import com.dozycoffee.wms.global.error.ErrorCode
import com.dozycoffee.wms.global.error.ErrorType

enum class InboundErrorCode(
    override val errorType: ErrorType,
    override val code: String,
    override val message: String
) : ErrorCode {

    INVALID_WAREHOUSE_ID(ErrorType.VALIDATION, "INBOUND_INVALID_WAREHOUSE_ID", "창고는 필수입니다."),
    INVALID_EXPECTED_ARRIVAL_DATE(
        ErrorType.VALIDATION,
        "INBOUND_INVALID_EXPECTED_ARRIVAL_DATE",
        "입고 예정일은 필수입니다."
    ),
    INVALID_STATUS_TRANSITION(
        ErrorType.CONFLICT,
        "INBOUND_INVALID_STATUS_TRANSITION",
        "입고 상태를 역행하거나 건너뛸 수 없습니다."
    ),
    INBOUND_NOT_FOUND(ErrorType.NOT_FOUND, "INBOUND_NOT_FOUND", "존재하지 않는 입고입니다.")
}
