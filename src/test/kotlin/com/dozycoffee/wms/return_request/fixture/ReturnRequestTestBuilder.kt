package com.dozycoffee.wms.return_request.fixture

import com.dozycoffee.wms.return_request.domain.enumeration.ReturnRequestStatus
import com.dozycoffee.wms.return_request.domain.model.ReturnRequest

class ReturnRequestTestBuilder {

    private var returnRequestId: Long? = null
    private var warehouseId: Long? = 1L
    private var status: ReturnRequestStatus = ReturnRequestStatus.RECEIVED

    companion object {
        fun returnRequest(): ReturnRequestTestBuilder = ReturnRequestTestBuilder()
    }

    fun returnRequestId(returnRequestId: Long?): ReturnRequestTestBuilder {
        this.returnRequestId = returnRequestId
        return this
    }

    fun warehouseId(warehouseId: Long?): ReturnRequestTestBuilder {
        this.warehouseId = warehouseId
        return this
    }

    fun status(status: ReturnRequestStatus): ReturnRequestTestBuilder {
        this.status = status
        return this
    }

    fun build(): ReturnRequest {
        val id: Long? = returnRequestId
        if (id != null) {
            return ReturnRequest.reconstitute(
                returnRequestId = id,
                warehouseId = requireNotNull(warehouseId) { "warehouseId는 재구성 시 필수입니다." },
                status = status
            )
        }
        return ReturnRequest.create(warehouseId = warehouseId)
    }
}
