package com.dozycoffee.wms.disposal.domain.exception

import com.dozycoffee.wms.global.error.ErrorCode
import com.dozycoffee.wms.global.error.ErrorType

enum class DisposalErrorCode(
    override val errorType: ErrorType,
    override val code: String,
    override val message: String
) : ErrorCode {

    INVALID_WAREHOUSE_ID(ErrorType.VALIDATION, "DISPOSAL_INVALID_WAREHOUSE_ID", "창고는 필수입니다."),
    INVALID_STATUS_TRANSITION(
        ErrorType.CONFLICT,
        "DISPOSAL_INVALID_STATUS_TRANSITION",
        "폐기 상태를 역행하거나 건너뛸 수 없습니다."
    ),
    DISPOSAL_NOT_FOUND(ErrorType.NOT_FOUND, "DISPOSAL_NOT_FOUND", "존재하지 않는 폐기입니다.")
}
