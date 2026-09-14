package com.dozycoffee.wms.inbound.domain.model

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.global.error.DomainValidator.requireNonNull
import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.inbound.domain.enumeration.InspectionResult
import com.dozycoffee.wms.inbound.domain.exception.InboundItemAlreadyInspectedException
import com.dozycoffee.wms.inbound.domain.exception.InboundItemErrorCode
import com.dozycoffee.wms.inbound.domain.exception.InvalidActualQuantityException
import com.dozycoffee.wms.inbound.domain.exception.InvalidInspectionResultException

class InboundItem private constructor(
    val inboundItemId: Long?,
    val inboundId: Long,
    val productId: Long,
    val zoneId: Long,
    val expectedQuantity: Int,
    actualQuantity: Int?,
    inspectionResult: InspectionResult
) : BaseEntity() {

    var actualQuantity: Int? = actualQuantity
        private set

    var inspectionResult: InspectionResult = inspectionResult
        private set

    /** 예정 수량과 실제 입고 수량의 차이 — 검수 전에는 null */
    val quantityDiscrepancy: Int?
        get() = actualQuantity?.let { it - expectedQuantity }

    companion object {

        fun create(
            inboundId: Long?,
            productId: Long?,
            zoneId: Long?,
            expectedQuantity: Int
        ): InboundItem {
            val validInboundId: Long = requireNonNull(inboundId, InboundItemErrorCode.INVALID_INBOUND_ID)
            val validProductId: Long = requireNonNull(productId, InboundItemErrorCode.INVALID_PRODUCT_ID)
            val validZoneId: Long = requireNonNull(zoneId, InboundItemErrorCode.INVALID_ZONE_ID)
            validateExpectedQuantity(expectedQuantity)
            return InboundItem(
                inboundItemId = null,
                inboundId = validInboundId,
                productId = validProductId,
                zoneId = validZoneId,
                expectedQuantity = expectedQuantity,
                actualQuantity = null,
                inspectionResult = InspectionResult.PENDING
            )
        }

        fun reconstitute(
            inboundItemId: Long,
            inboundId: Long,
            productId: Long,
            zoneId: Long,
            expectedQuantity: Int,
            actualQuantity: Int?,
            inspectionResult: InspectionResult
        ): InboundItem {
            return InboundItem(
                inboundItemId,
                inboundId,
                productId,
                zoneId,
                expectedQuantity,
                actualQuantity,
                inspectionResult
            )
        }

        private fun validateExpectedQuantity(expectedQuantity: Int) {
            if (expectedQuantity <= 0) {
                throw InvalidDomainValueException(InboundItemErrorCode.INVALID_EXPECTED_QUANTITY)
            }
        }
    }

    /** 실제 입고 수량과 파손·유통기한·품질 확인 결과를 기록해 정상/불량을 확정한다 */
    fun inspect(actualQuantity: Int, result: InspectionResult) {
        validateNotAlreadyInspected()
        validateActualQuantity(actualQuantity)
        validateInspectionResult(result)
        this.actualQuantity = actualQuantity
        this.inspectionResult = result
    }

    private fun validateNotAlreadyInspected() {
        if (inspectionResult != InspectionResult.PENDING) {
            throw InboundItemAlreadyInspectedException()
        }
    }

    private fun validateActualQuantity(actualQuantity: Int) {
        if (actualQuantity < 0) {
            throw InvalidActualQuantityException()
        }
    }

    private fun validateInspectionResult(result: InspectionResult) {
        if (result == InspectionResult.PENDING) {
            throw InvalidInspectionResultException()
        }
    }
}
