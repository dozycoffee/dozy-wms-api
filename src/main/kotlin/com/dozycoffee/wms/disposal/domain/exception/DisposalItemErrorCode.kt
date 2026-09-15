package com.dozycoffee.wms.disposal.domain.exception

import com.dozycoffee.wms.global.error.ErrorCode
import com.dozycoffee.wms.global.error.ErrorType

enum class DisposalItemErrorCode(
    override val errorType: ErrorType,
    override val code: String,
    override val message: String
) : ErrorCode {

    INVALID_DISPOSAL_ID(ErrorType.VALIDATION, "DISPOSAL_ITEM_INVALID_DISPOSAL_ID", "폐기는 필수입니다."),
    INVALID_INVENTORY_ID(ErrorType.VALIDATION, "DISPOSAL_ITEM_INVALID_INVENTORY_ID", "재고는 필수입니다."),
    INVALID_QUANTITY(
        ErrorType.VALIDATION,
        "DISPOSAL_ITEM_INVALID_QUANTITY",
        "폐기 수량은 0보다 커야 합니다."
    ),
    INVENTORY_NOT_DISPOSABLE(
        ErrorType.CONFLICT,
        "DISPOSAL_ITEM_INVENTORY_NOT_DISPOSABLE",
        "정상 품질 재고는 폐기 대상으로 등록할 수 없습니다."
    ),
    QUANTITY_MISMATCH(
        ErrorType.CONFLICT,
        "DISPOSAL_ITEM_QUANTITY_MISMATCH",
        "폐기 수량은 대상 재고의 수량과 일치해야 합니다."
    ),
    DISPOSAL_ITEM_NOT_FOUND(ErrorType.NOT_FOUND, "DISPOSAL_ITEM_NOT_FOUND", "존재하지 않는 폐기 상품입니다.")
}
