package com.dozycoffee.wms.return_request.domain.exception

import com.dozycoffee.wms.global.error.ErrorCode
import com.dozycoffee.wms.global.error.ErrorType

enum class ReturnItemErrorCode(
    override val errorType: ErrorType,
    override val code: String,
    override val message: String
) : ErrorCode {

    INVALID_RETURN_REQUEST_ID(
        ErrorType.VALIDATION,
        "RETURN_ITEM_INVALID_RETURN_REQUEST_ID",
        "반품은 필수입니다."
    ),
    INVALID_PRODUCT_ID(ErrorType.VALIDATION, "RETURN_ITEM_INVALID_PRODUCT_ID", "상품은 필수입니다."),
    INVALID_EXPECTED_QUANTITY(
        ErrorType.VALIDATION,
        "RETURN_ITEM_INVALID_EXPECTED_QUANTITY",
        "반품 예정 수량은 0보다 커야 합니다."
    ),
    INVALID_ACTUAL_QUANTITY(
        ErrorType.VALIDATION,
        "RETURN_ITEM_INVALID_ACTUAL_QUANTITY",
        "실제 반품 수량은 0 이상이어야 합니다."
    ),
    INVALID_INSPECTION_RESULT(
        ErrorType.VALIDATION,
        "RETURN_ITEM_INVALID_INSPECTION_RESULT",
        "검수 결과는 정상 또는 불량이어야 합니다."
    ),
    ALREADY_INSPECTED(
        ErrorType.CONFLICT,
        "RETURN_ITEM_ALREADY_INSPECTED",
        "이미 검수가 완료된 반품 상품입니다."
    ),
    MISSING_LOT_ASSIGNMENT(
        ErrorType.VALIDATION,
        "RETURN_ITEM_MISSING_LOT_ASSIGNMENT",
        "검수 완료된 반품 상품에는 Lot 정보가 필요합니다."
    ),
    RETURN_ITEM_NOT_FOUND(ErrorType.NOT_FOUND, "RETURN_ITEM_NOT_FOUND", "존재하지 않는 반품 상품입니다.")
}
