package com.dozycoffee.wms.inventory.domain.exception

import com.dozycoffee.wms.global.error.ErrorCode
import com.dozycoffee.wms.global.error.ErrorType

enum class InventoryErrorCode(
    override val errorType: ErrorType,
    override val code: String,
    override val message: String
) : ErrorCode {

    INVALID_PRODUCT_ID(ErrorType.VALIDATION, "INVENTORY_INVALID_PRODUCT_ID", "상품은 필수입니다."),
    INVALID_LOT_ID(ErrorType.VALIDATION, "INVENTORY_INVALID_LOT_ID", "Lot은 필수입니다."),
    INVALID_LOCATION_ID(ErrorType.VALIDATION, "INVENTORY_INVALID_LOCATION_ID", "위치는 필수입니다."),
    INVALID_QUANTITY(ErrorType.VALIDATION, "INVENTORY_INVALID_QUANTITY", "재고 수량은 0보다 커야 합니다."),
    INVALID_STATUS_COMBINATION(
        ErrorType.CONFLICT,
        "INVENTORY_INVALID_STATUS_COMBINATION",
        "정상 품질이 아닌 재고는 할당할 수 없습니다."
    ),
    INVENTORY_NOT_FOUND(ErrorType.NOT_FOUND, "INVENTORY_NOT_FOUND", "존재하지 않는 재고입니다.")
}
