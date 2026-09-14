package com.dozycoffee.wms.inventory.domain.exception

import com.dozycoffee.wms.global.error.ErrorCode
import com.dozycoffee.wms.global.error.ErrorType

enum class LotErrorCode(
    override val errorType: ErrorType,
    override val code: String,
    override val message: String
) : ErrorCode {

    INVALID_LOT_NUMBER(ErrorType.VALIDATION, "LOT_INVALID_LOT_NUMBER", "Lot 번호는 필수입니다."),
    INVALID_PRODUCT_ID(ErrorType.VALIDATION, "LOT_INVALID_PRODUCT_ID", "상품은 필수입니다."),
    INVALID_EXPIRATION_DATE(
        ErrorType.VALIDATION,
        "LOT_INVALID_EXPIRATION_DATE",
        "유통기한은 제조일자보다 빠를 수 없습니다."
    ),
    INVALID_STATUS_TRANSITION(ErrorType.CONFLICT, "LOT_INVALID_STATUS_TRANSITION", "Lot 상태를 역행하거나 건너뛸 수 없습니다."),
    LOT_NOT_FOUND(ErrorType.NOT_FOUND, "LOT_NOT_FOUND", "존재하지 않는 Lot입니다.")
}
