package com.dozycoffee.wms.stock_audit.domain.exception

import com.dozycoffee.wms.global.error.ErrorCode
import com.dozycoffee.wms.global.error.ErrorType

enum class StockAuditErrorCode(
    override val errorType: ErrorType,
    override val code: String,
    override val message: String
) : ErrorCode {

    INVALID_WAREHOUSE_ID(ErrorType.VALIDATION, "STOCK_AUDIT_INVALID_WAREHOUSE_ID", "창고는 필수입니다."),
    INVALID_ZONE_ID(ErrorType.VALIDATION, "STOCK_AUDIT_INVALID_ZONE_ID", "실사 대상 Zone은 필수입니다."),
    INVALID_ASSIGNEE(ErrorType.VALIDATION, "STOCK_AUDIT_INVALID_ASSIGNEE", "담당자는 필수입니다."),
    INVALID_STATUS_TRANSITION(
        ErrorType.CONFLICT,
        "STOCK_AUDIT_INVALID_STATUS_TRANSITION",
        "실사 상태를 역행하거나 건너뛸 수 없습니다."
    ),
    APPROVAL_REQUIRED(
        ErrorType.CONFLICT,
        "STOCK_AUDIT_APPROVAL_REQUIRED",
        "조정 수량이 임계치를 초과해 승인자가 필요합니다."
    ),
    ITEMS_NOT_FULLY_COUNTED(
        ErrorType.CONFLICT,
        "STOCK_AUDIT_ITEMS_NOT_FULLY_COUNTED",
        "모든 실사 항목의 카운트가 완료되어야 합니다."
    ),
    STOCK_AUDIT_NOT_FOUND(ErrorType.NOT_FOUND, "STOCK_AUDIT_NOT_FOUND", "존재하지 않는 실사입니다.")
}
