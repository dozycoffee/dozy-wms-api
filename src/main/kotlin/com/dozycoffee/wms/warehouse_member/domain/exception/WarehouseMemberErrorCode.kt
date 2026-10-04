package com.dozycoffee.wms.warehouse_member.domain.exception

import com.dozycoffee.wms.global.error.ErrorCode
import com.dozycoffee.wms.global.error.ErrorType

enum class WarehouseMemberErrorCode(
    override val errorType: ErrorType,
    override val code: String,
    override val message: String
) : ErrorCode {

    INVALID_WAREHOUSE_ID(ErrorType.VALIDATION, "WAREHOUSE_MEMBER_INVALID_WAREHOUSE_ID", "창고는 필수입니다."),
    INVALID_PRINCIPAL_ID(ErrorType.VALIDATION, "WAREHOUSE_MEMBER_INVALID_PRINCIPAL_ID", "사용자는 필수입니다."),
    ALREADY_ASSIGNED(
        ErrorType.CONFLICT,
        "WAREHOUSE_MEMBER_ALREADY_ASSIGNED",
        "동시에 같은 사용자가 이미 배정되었습니다. 다시 조회해 주세요."
    ),
    WAREHOUSE_MEMBER_NOT_FOUND(ErrorType.NOT_FOUND, "WAREHOUSE_MEMBER_NOT_FOUND", "창고에 배정되지 않은 사용자입니다.")
}
