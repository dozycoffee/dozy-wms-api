package com.dozycoffee.wms.inbound.domain.model

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.global.error.DomainValidator.requireNonNull
import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.inbound.domain.enumeration.DefectReason
import com.dozycoffee.wms.inbound.domain.enumeration.InspectionResult
import com.dozycoffee.wms.inbound.domain.exception.ExpiredReceiptCannotBeNormalException
import com.dozycoffee.wms.inbound.domain.exception.InboundReceiptErrorCode
import java.time.LocalDate

/** 검수에서 확정한 수령 라인 — 한 입고 상품의 로트·수량·판정 단위 */
class InboundReceipt private constructor(
    val inboundReceiptId: Long?,
    val inboundItemId: Long,
    val lotNumber: String,
    val manufactureDate: LocalDate?,
    val expirationDate: LocalDate?,
    val quantity: Int,
    val inspectionResult: InspectionResult,
    val defectReason: DefectReason?
) : BaseEntity() {

    companion object {

        fun create(
            inboundItemId: Long?,
            lotNumber: String?,
            manufactureDate: LocalDate?,
            expirationDate: LocalDate?,
            quantity: Int,
            inspectionResult: InspectionResult?,
            defectReason: DefectReason?,
            today: LocalDate
        ): InboundReceipt {
            val validInboundItemId: Long = requireNonNull(inboundItemId, InboundReceiptErrorCode.INVALID_INBOUND_ITEM_ID)
            val validLotNumber: String = validateLotNumber(lotNumber)
            val validResult: InspectionResult = requireNonNull(inspectionResult, InboundReceiptErrorCode.INVALID_INSPECTION_RESULT)
            validateQuantity(quantity)
            validateDates(manufactureDate, expirationDate, today)
            validateDefectReason(validResult, defectReason)
            validateNotExpiredIfNormal(validResult, expirationDate, today)
            return InboundReceipt(
                inboundReceiptId = null,
                inboundItemId = validInboundItemId,
                lotNumber = validLotNumber,
                manufactureDate = manufactureDate,
                expirationDate = expirationDate,
                quantity = quantity,
                inspectionResult = validResult,
                defectReason = defectReason
            )
        }

        fun reconstitute(
            inboundReceiptId: Long,
            inboundItemId: Long,
            lotNumber: String,
            manufactureDate: LocalDate?,
            expirationDate: LocalDate?,
            quantity: Int,
            inspectionResult: InspectionResult,
            defectReason: DefectReason?
        ): InboundReceipt {
            return InboundReceipt(
                inboundReceiptId,
                inboundItemId,
                lotNumber,
                manufactureDate,
                expirationDate,
                quantity,
                inspectionResult,
                defectReason
            )
        }

        private fun validateLotNumber(lotNumber: String?): String {
            if (lotNumber.isNullOrBlank()) {
                throw InvalidDomainValueException(InboundReceiptErrorCode.INVALID_LOT_NUMBER)
            }
            return lotNumber
        }

        private fun validateQuantity(quantity: Int) {
            if (quantity <= 0) {
                throw InvalidDomainValueException(InboundReceiptErrorCode.INVALID_QUANTITY)
            }
        }

        private fun validateDates(manufactureDate: LocalDate?, expirationDate: LocalDate?, today: LocalDate) {
            if (manufactureDate != null && manufactureDate.isAfter(today)) {
                throw InvalidDomainValueException(InboundReceiptErrorCode.MANUFACTURE_DATE_IN_FUTURE)
            }
            if (manufactureDate != null && expirationDate != null && manufactureDate.isAfter(expirationDate)) {
                throw InvalidDomainValueException(InboundReceiptErrorCode.INVALID_DATE_RANGE)
            }
        }

        private fun validateDefectReason(result: InspectionResult, defectReason: DefectReason?) {
            if (result == InspectionResult.DEFECTIVE && defectReason == null) {
                throw InvalidDomainValueException(InboundReceiptErrorCode.DEFECT_REASON_REQUIRED)
            }
            if (result == InspectionResult.NORMAL && defectReason != null) {
                throw InvalidDomainValueException(InboundReceiptErrorCode.DEFECT_REASON_NOT_ALLOWED)
            }
        }

        private fun validateNotExpiredIfNormal(result: InspectionResult, expirationDate: LocalDate?, today: LocalDate) {
            if (result == InspectionResult.NORMAL && expirationDate != null && expirationDate.isBefore(today)) {
                throw ExpiredReceiptCannotBeNormalException()
            }
        }
    }
}
