package com.dozycoffee.wms.stock_audit.domain.exception

import com.dozycoffee.wms.global.error.ErrorCode
import com.dozycoffee.wms.global.error.ErrorType

enum class StockAuditItemErrorCode(
    override val errorType: ErrorType,
    override val code: String,
    override val message: String
) : ErrorCode {

    INVALID_STOCK_AUDIT_ID(ErrorType.VALIDATION, "STOCK_AUDIT_ITEM_INVALID_STOCK_AUDIT_ID", "실사는 필수입니다."),
    INVALID_INVENTORY_ID(ErrorType.VALIDATION, "STOCK_AUDIT_ITEM_INVALID_INVENTORY_ID", "재고는 필수입니다."),
    INVALID_SNAPSHOT_QUANTITY(
        ErrorType.VALIDATION,
        "STOCK_AUDIT_ITEM_INVALID_SNAPSHOT_QUANTITY",
        "스냅샷 수량은 0 이상이어야 합니다."
    ),
    INVALID_COUNTED_QUANTITY(
        ErrorType.VALIDATION,
        "STOCK_AUDIT_ITEM_INVALID_COUNTED_QUANTITY",
        "실사 수량은 0 이상이어야 합니다."
    ),
    STOCK_AUDIT_ITEM_NOT_FOUND(ErrorType.NOT_FOUND, "STOCK_AUDIT_ITEM_NOT_FOUND", "존재하지 않는 실사 항목입니다.")
}
