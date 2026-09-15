package com.dozycoffee.wms.outbound.domain.exception

import com.dozycoffee.wms.global.error.ErrorCode
import com.dozycoffee.wms.global.error.ErrorType

enum class OutboundItemErrorCode(
    override val errorType: ErrorType,
    override val code: String,
    override val message: String
) : ErrorCode {

    INVALID_OUTBOUND_ID(ErrorType.VALIDATION, "OUTBOUND_ITEM_INVALID_OUTBOUND_ID", "출고는 필수입니다."),
    INVALID_PRODUCT_ID(ErrorType.VALIDATION, "OUTBOUND_ITEM_INVALID_PRODUCT_ID", "상품은 필수입니다."),
    INVALID_REQUESTED_QUANTITY(
        ErrorType.VALIDATION,
        "OUTBOUND_ITEM_INVALID_REQUESTED_QUANTITY",
        "출고 요청 수량은 0보다 커야 합니다."
    ),
    INVALID_PICKED_QUANTITY(
        ErrorType.VALIDATION,
        "OUTBOUND_ITEM_INVALID_PICKED_QUANTITY",
        "피킹 수량은 0 이상 요청 수량 이하여야 합니다."
    ),
    ALREADY_PICKED(
        ErrorType.CONFLICT,
        "OUTBOUND_ITEM_ALREADY_PICKED",
        "이미 피킹이 완료된 출고 상품입니다."
    ),
    OUTBOUND_ITEM_NOT_FOUND(ErrorType.NOT_FOUND, "OUTBOUND_ITEM_NOT_FOUND", "존재하지 않는 출고 상품입니다.")
}
