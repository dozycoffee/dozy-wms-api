package com.dozycoffee.wms.inbound.domain.exception

import com.dozycoffee.wms.global.error.ErrorCode
import com.dozycoffee.wms.global.error.ErrorType

enum class InboundReceiptErrorCode(
    override val errorType: ErrorType,
    override val code: String,
    override val message: String
) : ErrorCode {

    INVALID_INBOUND_ITEM_ID(ErrorType.VALIDATION, "INBOUND_RECEIPT_INVALID_INBOUND_ITEM_ID", "입고 상품은 필수입니다."),
    INVALID_LOT_NUMBER(ErrorType.VALIDATION, "INBOUND_RECEIPT_INVALID_LOT_NUMBER", "로트 번호는 필수입니다."),
    INVALID_QUANTITY(ErrorType.VALIDATION, "INBOUND_RECEIPT_INVALID_QUANTITY", "수령 수량은 0보다 커야 합니다."),
    INVALID_INSPECTION_RESULT(
        ErrorType.VALIDATION,
        "INBOUND_RECEIPT_INVALID_INSPECTION_RESULT",
        "검수 결과는 필수입니다."
    ),
    DEFECT_REASON_REQUIRED(
        ErrorType.VALIDATION,
        "INBOUND_RECEIPT_DEFECT_REASON_REQUIRED",
        "불량 판정에는 불량 사유가 필요합니다."
    ),
    DEFECT_REASON_NOT_ALLOWED(
        ErrorType.VALIDATION,
        "INBOUND_RECEIPT_DEFECT_REASON_NOT_ALLOWED",
        "정상 판정에는 불량 사유를 지정할 수 없습니다."
    ),
    INVALID_DATE_RANGE(
        ErrorType.VALIDATION,
        "INBOUND_RECEIPT_INVALID_DATE_RANGE",
        "제조일자는 유통기한보다 늦을 수 없습니다."
    ),
    MANUFACTURE_DATE_IN_FUTURE(
        ErrorType.VALIDATION,
        "INBOUND_RECEIPT_MANUFACTURE_DATE_IN_FUTURE",
        "제조일자는 오늘보다 늦을 수 없습니다."
    ),
    EXPIRED_CANNOT_BE_NORMAL(
        ErrorType.CONFLICT,
        "INBOUND_RECEIPT_EXPIRED_CANNOT_BE_NORMAL",
        "유통기한이 지난 상품은 정상으로 판정할 수 없습니다."
    ),
    EXPIRATION_DATE_REQUIRED(
        ErrorType.VALIDATION,
        "INBOUND_RECEIPT_EXPIRATION_DATE_REQUIRED",
        "유통기한 관리 상품은 유통기한이 필요합니다."
    ),
    EXPIRATION_DATE_NOT_ALLOWED(
        ErrorType.VALIDATION,
        "INBOUND_RECEIPT_EXPIRATION_DATE_NOT_ALLOWED",
        "유통기한을 관리하지 않는 상품에는 유통기한을 지정할 수 없습니다."
    ),
    LOT_EXPIRATION_CONFLICT(
        ErrorType.CONFLICT,
        "INBOUND_RECEIPT_LOT_EXPIRATION_CONFLICT",
        "같은 로트 번호에 서로 다른 유통기한을 지정할 수 없습니다."
    ),
    OVER_RECEIVED(
        ErrorType.CONFLICT,
        "INBOUND_RECEIPT_OVER_RECEIVED",
        "수령 수량 합계가 입고 예정 수량을 초과할 수 없습니다."
    )
}
