package com.dozycoffee.wms.product.domain.exception

import com.dozycoffee.wms.global.error.ErrorCode
import com.dozycoffee.wms.global.error.ErrorType

enum class ProductErrorCode(
    override val errorType: ErrorType,
    override val code: String,
    override val message: String
) : ErrorCode {

    INVALID_PRODUCT_CODE(ErrorType.VALIDATION, "PRODUCT_INVALID_PRODUCT_CODE", "상품 코드는 필수입니다."),
    INVALID_PRODUCT_NAME(ErrorType.VALIDATION, "PRODUCT_INVALID_PRODUCT_NAME", "상품명은 필수입니다."),
    INVALID_CATEGORY(ErrorType.VALIDATION, "PRODUCT_INVALID_CATEGORY", "상품 카테고리는 필수입니다."),
    INVALID_UNIT(ErrorType.VALIDATION, "PRODUCT_INVALID_UNIT", "상품 단위는 필수입니다."),
    INVALID_SHELF_LIFE_DAYS(
        ErrorType.VALIDATION,
        "PRODUCT_INVALID_SHELF_LIFE_DAYS",
        "유통기한 일수는 0 이상이어야 합니다."
    ),
    PRODUCT_NOT_FOUND(ErrorType.NOT_FOUND, "PRODUCT_NOT_FOUND", "존재하지 않는 상품입니다.")
}
