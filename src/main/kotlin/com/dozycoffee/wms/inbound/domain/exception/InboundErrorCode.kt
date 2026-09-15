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
    INSUFFICIENT_ZONE_CAPACITY(
        ErrorType.CONFLICT,
        "INBOUND_INSUFFICIENT_ZONE_CAPACITY",
        "Zone의 잔여 용량이 부족해 입고를 등록할 수 없습니다."
    ),
    NOT_ALL_ITEMS_INSPECTED(
        ErrorType.CONFLICT,
        "INBOUND_NOT_ALL_ITEMS_INSPECTED",
        "검수가 완료되지 않은 입고 상품이 있어 입고를 완료할 수 없습니다."
    ),
    INBOUND_NOT_FOUND(ErrorType.NOT_FOUND, "INBOUND_NOT_FOUND", "존재하지 않는 입고입니다.")
}
