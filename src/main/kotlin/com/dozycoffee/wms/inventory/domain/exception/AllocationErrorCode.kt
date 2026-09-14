package com.dozycoffee.wms.inventory.domain.exception

import com.dozycoffee.wms.global.error.ErrorCode
import com.dozycoffee.wms.global.error.ErrorType

enum class AllocationErrorCode(
    override val errorType: ErrorType,
    override val code: String,
    override val message: String
) : ErrorCode {

    INVALID_INVENTORY_ID(ErrorType.VALIDATION, "ALLOCATION_INVALID_INVENTORY_ID", "재고는 필수입니다."),
    INVALID_REFERENCE_TYPE(ErrorType.VALIDATION, "ALLOCATION_INVALID_REFERENCE_TYPE", "참조 유형은 필수입니다."),
    INVALID_REFERENCE_ID(ErrorType.VALIDATION, "ALLOCATION_INVALID_REFERENCE_ID", "참조 ID는 필수입니다."),
    INVALID_QUANTITY(ErrorType.VALIDATION, "ALLOCATION_INVALID_QUANTITY", "점유 수량은 0보다 커야 합니다."),
    INVALID_STATUS_TRANSITION(
        ErrorType.CONFLICT,
        "ALLOCATION_INVALID_STATUS_TRANSITION",
        "이미 종결된 점유는 상태를 변경할 수 없습니다."
    ),
    ALLOCATION_NOT_FOUND(ErrorType.NOT_FOUND, "ALLOCATION_NOT_FOUND", "존재하지 않는 점유입니다.")
}
