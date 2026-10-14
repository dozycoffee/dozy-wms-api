package com.dozycoffee.wms.outbound.domain.exception

import com.dozycoffee.wms.global.error.ErrorCode
import com.dozycoffee.wms.global.error.ErrorType

enum class OutboundErrorCode(
    override val errorType: ErrorType,
    override val code: String,
    override val message: String
) : ErrorCode {

    INVALID_WAREHOUSE_ID(ErrorType.VALIDATION, "OUTBOUND_INVALID_WAREHOUSE_ID", "창고는 필수입니다."),
    INVALID_RESERVATION_ID(ErrorType.VALIDATION, "OUTBOUND_INVALID_RESERVATION_ID", "예약은 필수입니다."),
    INVALID_STATUS_TRANSITION(
        ErrorType.CONFLICT,
        "OUTBOUND_INVALID_STATUS_TRANSITION",
        "출고 상태를 역행하거나 건너뛸 수 없습니다."
    ),
    NOT_REALLOCATABLE(
        ErrorType.CONFLICT,
        "OUTBOUND_NOT_REALLOCATABLE",
        "예약 정보가 있고 피킹 중인 출고만 재할당할 수 있습니다."
    ),
    INVALID_REALLOCATION(
        ErrorType.VALIDATION,
        "OUTBOUND_INVALID_REALLOCATION",
        "재할당 요청이 올바르지 않습니다. 이 출고의 할당을 한 번씩만 지정하고 부족 수량은 1 이상 할당 수량 이하여야 합니다."
    ),
    INVENTORY_REJECTED(
        ErrorType.CONFLICT,
        "OUTBOUND_INVENTORY_REJECTED",
        "inventory가 재할당을 거부했습니다. 대체할 재고가 모자라거나 예약 상태가 바뀌었는지 확인해 주세요."
    ),
    INVENTORY_UNAVAILABLE(
        ErrorType.UNAVAILABLE,
        "OUTBOUND_INVENTORY_UNAVAILABLE",
        "inventory와 통신할 수 없어 재할당하지 못했습니다. 잠시 후 다시 시도해 주세요."
    ),
    OUTBOUND_NOT_FOUND(ErrorType.NOT_FOUND, "OUTBOUND_NOT_FOUND", "존재하지 않는 출고입니다.")
}
