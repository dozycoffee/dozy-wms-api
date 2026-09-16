package com.dozycoffee.wms.inventory.domain.exception

import com.dozycoffee.wms.global.error.ErrorCode
import com.dozycoffee.wms.global.error.ErrorType

enum class InventoryHistoryErrorCode(
    override val errorType: ErrorType,
    override val code: String,
    override val message: String
) : ErrorCode {

    INVALID_INVENTORY_ID(ErrorType.VALIDATION, "INVENTORY_HISTORY_INVALID_INVENTORY_ID", "재고는 필수입니다."),
    INVALID_HISTORY_TYPE(ErrorType.VALIDATION, "INVENTORY_HISTORY_INVALID_HISTORY_TYPE", "이력 유형은 필수입니다."),
    INVALID_REFERENCE_ID(ErrorType.VALIDATION, "INVENTORY_HISTORY_INVALID_REFERENCE_ID", "참조 ID는 필수입니다."),
    INVALID_QUANTITY_CHANGE(
        ErrorType.VALIDATION,
        "INVENTORY_HISTORY_INVALID_QUANTITY_CHANGE",
        "변화량은 0이 될 수 없습니다."
    )
}
