package com.dozycoffee.wms.return_request.domain.model

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.global.error.DomainValidator.requireNonNull
import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.return_request.domain.enumeration.ReturnInspectionResult
import com.dozycoffee.wms.return_request.domain.exception.InvalidActualQuantityException
import com.dozycoffee.wms.return_request.domain.exception.InvalidInspectionResultException
import com.dozycoffee.wms.return_request.domain.exception.ReturnItemAlreadyInspectedException
import com.dozycoffee.wms.return_request.domain.exception.ReturnItemErrorCode

class ReturnItem private constructor(
    val returnItemId: Long?,
    val returnRequestId: Long,
    val productId: Long,
    val expectedQuantity: Int,
    actualQuantity: Int?,
    inspectionResult: ReturnInspectionResult
) : BaseEntity() {

    var actualQuantity: Int? = actualQuantity
        private set

    var inspectionResult: ReturnInspectionResult = inspectionResult
        private set

    /** 반품 신고 수량과 실제 검수 수량의 차이 — 검수 전에는 null */
    val quantityDiscrepancy: Int?
        get() = actualQuantity?.let { it - expectedQuantity }

    companion object {

        fun create(returnRequestId: Long?, productId: Long?, expectedQuantity: Int): ReturnItem {
            val validReturnRequestId: Long =
                requireNonNull(returnRequestId, ReturnItemErrorCode.INVALID_RETURN_REQUEST_ID)
            val validProductId: Long = requireNonNull(productId, ReturnItemErrorCode.INVALID_PRODUCT_ID)
            validateExpectedQuantity(expectedQuantity)
            return ReturnItem(
                returnItemId = null,
                returnRequestId = validReturnRequestId,
                productId = validProductId,
                expectedQuantity = expectedQuantity,
                actualQuantity = null,
                inspectionResult = ReturnInspectionResult.PENDING
            )
        }

        fun reconstitute(
            returnItemId: Long,
            returnRequestId: Long,
            productId: Long,
            expectedQuantity: Int,
            actualQuantity: Int?,
            inspectionResult: ReturnInspectionResult
        ): ReturnItem {
            return ReturnItem(
                returnItemId,
                returnRequestId,
                productId,
                expectedQuantity,
                actualQuantity,
                inspectionResult
            )
        }

        private fun validateExpectedQuantity(expectedQuantity: Int) {
            if (expectedQuantity <= 0) {
                throw InvalidDomainValueException(ReturnItemErrorCode.INVALID_EXPECTED_QUANTITY)
            }
        }
    }

    /** 실제 반품 수량과 파손·품질 확인 결과를 기록해 정상/불량을 확정한다 */
    fun inspect(actualQuantity: Int, result: ReturnInspectionResult) {
        validateNotAlreadyInspected()
        validateActualQuantity(actualQuantity)
        validateInspectionResult(result)
        this.actualQuantity = actualQuantity
        this.inspectionResult = result
    }

    private fun validateNotAlreadyInspected() {
        if (inspectionResult != ReturnInspectionResult.PENDING) {
            throw ReturnItemAlreadyInspectedException()
        }
    }

    private fun validateActualQuantity(actualQuantity: Int) {
        if (actualQuantity < 0) {
            throw InvalidActualQuantityException()
        }
    }

    private fun validateInspectionResult(result: ReturnInspectionResult) {
        if (result == ReturnInspectionResult.PENDING) {
            throw InvalidInspectionResultException()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReturnItem) return false
        val id: Long? = returnItemId
        return id != null && id == other.returnItemId
    }

    override fun hashCode(): Int {
        return returnItemId?.hashCode() ?: System.identityHashCode(this)
    }
}
