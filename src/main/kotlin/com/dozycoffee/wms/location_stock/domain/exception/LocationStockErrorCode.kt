package com.dozycoffee.wms.location_stock.domain.exception

import com.dozycoffee.wms.global.error.ErrorCode
import com.dozycoffee.wms.global.error.ErrorType

enum class LocationStockErrorCode(
    override val errorType: ErrorType,
    override val code: String,
    override val message: String
) : ErrorCode {

    INVALID_LOCATION_ID(ErrorType.VALIDATION, "LOCATION_STOCK_INVALID_LOCATION_ID", "위치는 필수입니다."),
    INVALID_PRODUCT_ID(ErrorType.VALIDATION, "LOCATION_STOCK_INVALID_PRODUCT_ID", "상품은 필수입니다."),
    INVALID_LOT_NUMBER(ErrorType.VALIDATION, "LOCATION_STOCK_INVALID_LOT_NUMBER", "Lot 번호는 비어 있을 수 없고 50자 이하여야 합니다."),
    INVALID_AMOUNT(ErrorType.VALIDATION, "LOCATION_STOCK_INVALID_AMOUNT", "처리 수량은 0보다 커야 합니다."),
    SAME_QUALITY_STATUS(ErrorType.VALIDATION, "LOCATION_STOCK_SAME_QUALITY_STATUS", "전환 전후의 품질 상태가 같을 수 없습니다."),
    INSUFFICIENT_QUANTITY(ErrorType.CONFLICT, "LOCATION_STOCK_INSUFFICIENT_QUANTITY", "위치의 재고 수량이 부족합니다."),
    INSUFFICIENT_HELD_QUANTITY(ErrorType.CONFLICT, "LOCATION_STOCK_INSUFFICIENT_HELD_QUANTITY", "위치 재고의 점유 수량이 부족합니다."),
    LOCATION_STOCK_NOT_FOUND(ErrorType.NOT_FOUND, "LOCATION_STOCK_NOT_FOUND", "존재하지 않는 위치 재고입니다."),
    DUPLICATE_LOCATION_STOCK(ErrorType.CONFLICT, "LOCATION_STOCK_DUPLICATE", "이미 존재하는 위치 재고입니다.")
}
