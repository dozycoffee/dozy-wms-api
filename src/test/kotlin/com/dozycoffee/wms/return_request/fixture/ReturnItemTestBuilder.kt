package com.dozycoffee.wms.return_request.fixture

import com.dozycoffee.wms.return_request.domain.enumeration.ReturnInspectionResult
import com.dozycoffee.wms.return_request.domain.model.ReturnItem

class ReturnItemTestBuilder {

    private var returnItemId: Long? = null
    private var returnRequestId: Long? = 1L
    private var productId: Long? = 1L
    private var expectedQuantity: Int = 10
    private var actualQuantity: Int? = null
    private var inspectionResult: ReturnInspectionResult = ReturnInspectionResult.PENDING

    companion object {
        fun returnItem(): ReturnItemTestBuilder = ReturnItemTestBuilder()
    }

    fun returnItemId(returnItemId: Long?): ReturnItemTestBuilder {
        this.returnItemId = returnItemId
        return this
    }

    fun returnRequestId(returnRequestId: Long?): ReturnItemTestBuilder {
        this.returnRequestId = returnRequestId
        return this
    }

    fun productId(productId: Long?): ReturnItemTestBuilder {
        this.productId = productId
        return this
    }

    fun expectedQuantity(expectedQuantity: Int): ReturnItemTestBuilder {
        this.expectedQuantity = expectedQuantity
        return this
    }

    fun actualQuantity(actualQuantity: Int?): ReturnItemTestBuilder {
        this.actualQuantity = actualQuantity
        return this
    }

    fun inspectionResult(inspectionResult: ReturnInspectionResult): ReturnItemTestBuilder {
        this.inspectionResult = inspectionResult
        return this
    }

    fun build(): ReturnItem {
        val id: Long? = returnItemId
        if (id != null) {
            return ReturnItem.reconstitute(
                returnItemId = id,
                returnRequestId = requireNotNull(returnRequestId) { "returnRequestId는 재구성 시 필수입니다." },
                productId = requireNotNull(productId) { "productId는 재구성 시 필수입니다." },
                expectedQuantity = expectedQuantity,
                actualQuantity = actualQuantity,
                inspectionResult = inspectionResult
            )
        }
        return ReturnItem.create(
            returnRequestId = returnRequestId,
            productId = productId,
            expectedQuantity = expectedQuantity
        )
    }
}
