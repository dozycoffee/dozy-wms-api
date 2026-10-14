package com.dozycoffee.wms.outbound.domain.exception

import com.dozycoffee.wms.global.error.ErrorCode
import com.dozycoffee.wms.global.error.ErrorType

enum class OutboundAllocationErrorCode(
    override val errorType: ErrorType,
    override val code: String,
    override val message: String
) : ErrorCode {

    INVALID_OUTBOUND_ITEM_ID(ErrorType.VALIDATION, "OUTBOUND_ALLOCATION_INVALID_OUTBOUND_ITEM_ID", "출고 상품은 필수입니다."),
    INVALID_INVENTORY_ID(ErrorType.VALIDATION, "OUTBOUND_ALLOCATION_INVALID_INVENTORY_ID", "재고 행은 필수입니다."),
    INVALID_LOT_NUMBER(ErrorType.VALIDATION, "OUTBOUND_ALLOCATION_INVALID_LOT_NUMBER", "Lot 번호는 필수입니다."),
    INVALID_QUANTITY(ErrorType.VALIDATION, "OUTBOUND_ALLOCATION_INVALID_QUANTITY", "할당 수량은 0보다 커야 합니다."),
    INVALID_PICKED_QUANTITY(
        ErrorType.VALIDATION,
        "OUTBOUND_ALLOCATION_INVALID_PICKED_QUANTITY",
        "피킹 수량은 0 이상 할당 수량 이하여야 합니다."
    )
}
