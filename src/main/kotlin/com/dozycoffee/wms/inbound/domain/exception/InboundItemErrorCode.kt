package com.dozycoffee.wms.inbound.domain.exception

import com.dozycoffee.wms.global.error.ErrorCode
import com.dozycoffee.wms.global.error.ErrorType

enum class InboundItemErrorCode(
    override val errorType: ErrorType,
    override val code: String,
    override val message: String
) : ErrorCode {

    INVALID_INBOUND_ID(ErrorType.VALIDATION, "INBOUND_ITEM_INVALID_INBOUND_ID", "입고는 필수입니다."),
    INVALID_PRODUCT_ID(ErrorType.VALIDATION, "INBOUND_ITEM_INVALID_PRODUCT_ID", "상품은 필수입니다."),
    INVALID_ZONE_ID(ErrorType.VALIDATION, "INBOUND_ITEM_INVALID_ZONE_ID", "적재 Zone은 필수입니다."),
    INVALID_EXPECTED_QUANTITY(
        ErrorType.VALIDATION,
        "INBOUND_ITEM_INVALID_EXPECTED_QUANTITY",
        "입고 예정 수량은 0보다 커야 합니다."
    ),
    INVALID_ACTUAL_QUANTITY(
        ErrorType.VALIDATION,
        "INBOUND_ITEM_INVALID_ACTUAL_QUANTITY",
        "실제 입고 수량은 0 이상이어야 합니다."
    ),
    INVALID_INSPECTION_RESULT(
        ErrorType.VALIDATION,
        "INBOUND_ITEM_INVALID_INSPECTION_RESULT",
        "검수 결과는 정상 또는 불량이어야 합니다."
    ),
    ALREADY_INSPECTED(
        ErrorType.CONFLICT,
        "INBOUND_ITEM_ALREADY_INSPECTED",
        "이미 검수가 완료된 입고 상품입니다."
    ),
    MISSING_LOT_ASSIGNMENT(
        ErrorType.VALIDATION,
        "INBOUND_ITEM_MISSING_LOT_ASSIGNMENT",
        "정상 판정된 입고 상품에는 Lot 정보가 필요합니다."
    ),
    INBOUND_ITEM_NOT_FOUND(ErrorType.NOT_FOUND, "INBOUND_ITEM_NOT_FOUND", "존재하지 않는 입고 상품입니다.")
}
