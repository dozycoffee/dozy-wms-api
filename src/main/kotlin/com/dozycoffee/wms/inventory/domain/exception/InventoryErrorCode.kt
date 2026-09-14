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
    INVALID_AMOUNT(ErrorType.VALIDATION, "INVENTORY_INVALID_AMOUNT", "처리 수량은 0보다 커야 합니다."),
    NOT_ALLOCATABLE(ErrorType.CONFLICT, "INVENTORY_NOT_ALLOCATABLE", "정상 품질이 아닌 재고는 점유할 수 없습니다."),
    INSUFFICIENT_AVAILABLE_QUANTITY(
        ErrorType.CONFLICT,
        "INVENTORY_INSUFFICIENT_AVAILABLE_QUANTITY",
        "가용 수량이 부족합니다."
    ),
    INSUFFICIENT_HELD_QUANTITY(
        ErrorType.CONFLICT,
        "INVENTORY_INSUFFICIENT_HELD_QUANTITY",
        "해제/이행하려는 수량이 점유된 수량보다 많습니다."
    ),
    HAS_ACTIVE_ALLOCATION(
        ErrorType.CONFLICT,
        "INVENTORY_HAS_ACTIVE_ALLOCATION",
        "점유된 재고가 남아있어 품질 상태를 변경할 수 없습니다."
    ),
    INVENTORY_NOT_FOUND(ErrorType.NOT_FOUND, "INVENTORY_NOT_FOUND", "존재하지 않는 재고입니다.")
}
