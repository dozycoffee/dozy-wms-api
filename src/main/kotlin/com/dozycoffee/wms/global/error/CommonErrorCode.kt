package com.dozycoffee.wms.global.error

enum class CommonErrorCode(
    override val errorType: ErrorType,
    override val code: String,
    override val message: String
) : ErrorCode {

    INVALID_INPUT(ErrorType.VALIDATION, "COMMON_INVALID_INPUT", "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(ErrorType.INTERNAL, "COMMON_INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.")
}
