package com.dozycoffee.wms.inbound.domain.model

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.global.error.DomainValidator.requireNonNull
import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.inbound.domain.enumeration.InspectionStatus
import com.dozycoffee.wms.inbound.domain.exception.InboundItemAlreadyInspectedException
import com.dozycoffee.wms.inbound.domain.exception.InboundItemErrorCode
import com.dozycoffee.wms.inbound.domain.exception.InboundOverReceivedException
import com.dozycoffee.wms.inbound.domain.exception.LotExpirationConflictException
import java.time.LocalDate

class InboundItem private constructor(
    val inboundItemId: Long?,
    val inboundId: Long,
    val productId: Long,
    val zoneId: Long,
    val expectedQuantity: Int,
    val expectedLotNumber: String?,
    val expectedExpirationDate: LocalDate?,
    actualQuantity: Int?,
    inspectionStatus: InspectionStatus
) : BaseEntity() {

    var actualQuantity: Int? = actualQuantity
        private set

    var inspectionStatus: InspectionStatus = inspectionStatus
        private set

    /** 예정 수량과 실제 입고 수량의 차이 — 검수 전에는 null */
    val quantityDiscrepancy: Int?
        get() = actualQuantity?.let { it - expectedQuantity }

    companion object {

        fun create(
            inboundId: Long?,
            productId: Long?,
            zoneId: Long?,
            expectedQuantity: Int,
            expectedLotNumber: String? = null,
            expectedExpirationDate: LocalDate? = null
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
                expectedLotNumber = expectedLotNumber?.takeIf { it.isNotBlank() },
                expectedExpirationDate = expectedExpirationDate,
                actualQuantity = null,
                inspectionStatus = InspectionStatus.PENDING
            )
        }

        fun reconstitute(
            inboundItemId: Long,
            inboundId: Long,
            productId: Long,
            zoneId: Long,
            expectedQuantity: Int,
            expectedLotNumber: String?,
            expectedExpirationDate: LocalDate?,
            actualQuantity: Int?,
            inspectionStatus: InspectionStatus
        ): InboundItem {
            return InboundItem(
                inboundItemId,
                inboundId,
                productId,
                zoneId,
                expectedQuantity,
                expectedLotNumber,
                expectedExpirationDate,
                actualQuantity,
                inspectionStatus
            )
        }

        private fun validateExpectedQuantity(expectedQuantity: Int) {
            if (expectedQuantity <= 0) {
                throw InvalidDomainValueException(InboundItemErrorCode.INVALID_EXPECTED_QUANTITY)
            }
        }
    }

    /**
     * 수령 라인 전체를 한 번에 받아 검수를 확정한다. 빈 목록은 미도착(수령 0)이다.
     * 같은 로트 번호의 라인끼리는 유통기한이 같아야 하고, 수량 합이 예정 수량을 넘을 수 없다.
     */
    fun inspect(receipts: List<InboundReceipt>) {
        validateNotAlreadyInspected()
        validateConsistentLotExpiration(receipts)
        val totalQuantity: Int = receipts.sumOf { it.quantity }
        validateNotOverReceived(totalQuantity)
        this.actualQuantity = totalQuantity
        this.inspectionStatus = InspectionStatus.INSPECTED
    }

    private fun validateNotAlreadyInspected() {
        if (inspectionStatus != InspectionStatus.PENDING) {
            throw InboundItemAlreadyInspectedException()
        }
    }

    private fun validateConsistentLotExpiration(receipts: List<InboundReceipt>) {
        val inconsistent: Boolean = receipts.groupBy { it.lotNumber }
            .any { (_, lines) -> lines.map { it.expirationDate }.distinct().size > 1 }
        if (inconsistent) {
            throw LotExpirationConflictException()
        }
    }

    private fun validateNotOverReceived(totalQuantity: Int) {
        if (totalQuantity > expectedQuantity) {
            throw InboundOverReceivedException()
        }
    }
}
