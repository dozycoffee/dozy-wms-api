package com.dozycoffee.wms.return_request.domain.exception

import com.dozycoffee.wms.global.error.ErrorCode
import com.dozycoffee.wms.global.error.ErrorType

enum class ReturnRequestErrorCode(
    override val errorType: ErrorType,
    override val code: String,
    override val message: String
) : ErrorCode {

    INVALID_WAREHOUSE_ID(ErrorType.VALIDATION, "RETURN_REQUEST_INVALID_WAREHOUSE_ID", "창고는 필수입니다."),
    INVALID_STATUS_TRANSITION(
        ErrorType.CONFLICT,
        "RETURN_REQUEST_INVALID_STATUS_TRANSITION",
        "반품 상태를 역행하거나 건너뛸 수 없습니다."
    ),
    RETURN_REQUEST_NOT_FOUND(ErrorType.NOT_FOUND, "RETURN_REQUEST_NOT_FOUND", "존재하지 않는 반품입니다.")
}
