package com.dozycoffee.wms.inventory_call.domain.exception

import com.dozycoffee.wms.global.error.ErrorCode
import com.dozycoffee.wms.global.error.ErrorType

enum class InventoryCallErrorCode(
    override val errorType: ErrorType,
    override val code: String,
    override val message: String
) : ErrorCode {

    INVALID_CALL_TYPE(ErrorType.VALIDATION, "INVENTORY_CALL_INVALID_CALL_TYPE", "호출 종류는 필수입니다."),
    INVALID_WAREHOUSE_ID(ErrorType.VALIDATION, "INVENTORY_CALL_INVALID_WAREHOUSE_ID", "창고는 필수입니다."),
    INVALID_IDEMPOTENCY_KEY(
        ErrorType.VALIDATION,
        "INVENTORY_CALL_INVALID_IDEMPOTENCY_KEY",
        "멱등 키는 공백 없는 ASCII 100자 이하여야 합니다."
    ),
    INVALID_REQUEST_PATH(ErrorType.VALIDATION, "INVENTORY_CALL_INVALID_REQUEST_PATH", "요청 경로는 /로 시작하는 200자 이하여야 합니다."),
    INVALID_REQUEST_BODY(ErrorType.VALIDATION, "INVENTORY_CALL_INVALID_REQUEST_BODY", "요청 본문은 필수입니다."),
    INVALID_STATE_TRANSITION(ErrorType.CONFLICT, "INVENTORY_CALL_INVALID_STATE_TRANSITION", "현재 상태에서 허용되지 않는 처리입니다."),
    KEY_CONFLICT(ErrorType.CONFLICT, "INVENTORY_CALL_KEY_CONFLICT", "같은 멱등 키로 다른 내용의 호출이 이미 기록돼 있습니다."),
    NOT_FOUND(ErrorType.NOT_FOUND, "INVENTORY_CALL_NOT_FOUND", "존재하지 않는 inventory 호출입니다.")
}
