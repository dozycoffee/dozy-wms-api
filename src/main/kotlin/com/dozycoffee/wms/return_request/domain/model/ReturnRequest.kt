package com.dozycoffee.wms.return_request.domain.model

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.global.error.DomainValidator.requireNonNull
import com.dozycoffee.wms.return_request.domain.enumeration.ReturnRequestStatus
import com.dozycoffee.wms.return_request.domain.exception.InvalidReturnRequestStatusTransitionException
import com.dozycoffee.wms.return_request.domain.exception.ReturnRequestErrorCode

class ReturnRequest private constructor(
    val returnRequestId: Long?,
    val warehouseId: Long,
    status: ReturnRequestStatus
) : BaseEntity() {

    var status: ReturnRequestStatus = status
        private set

    companion object {

        fun create(warehouseId: Long?): ReturnRequest {
            val validWarehouseId: Long = requireNonNull(warehouseId, ReturnRequestErrorCode.INVALID_WAREHOUSE_ID)
            return ReturnRequest(
                returnRequestId = null,
                warehouseId = validWarehouseId,
                status = ReturnRequestStatus.RECEIVED
            )
        }

        fun reconstitute(returnRequestId: Long, warehouseId: Long, status: ReturnRequestStatus): ReturnRequest {
            return ReturnRequest(returnRequestId, warehouseId, status)
        }
    }

    /** 반품 상품이 반품 처리장에 도착해 검수를 시작할 때 호출한다 */
    fun startInspecting() = transitionTo(ReturnRequestStatus.INSPECTING)

    /** 검수 결과에 따른 재고/폐기 처리까지 마쳐 반품을 완료할 때 호출한다 */
    fun complete() = transitionTo(ReturnRequestStatus.COMPLETED)

    private fun transitionTo(target: ReturnRequestStatus) {
        if (!status.canTransitionTo(target)) {
            throw InvalidReturnRequestStatusTransitionException()
        }
        status = target
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReturnRequest) return false
        val id: Long? = returnRequestId
        return id != null && id == other.returnRequestId
    }

    override fun hashCode(): Int {
        return returnRequestId?.hashCode() ?: System.identityHashCode(this)
    }
}
