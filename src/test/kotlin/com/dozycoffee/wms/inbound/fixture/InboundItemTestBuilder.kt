package com.dozycoffee.wms.inbound.fixture

import com.dozycoffee.wms.inbound.domain.enumeration.InspectionResult
import com.dozycoffee.wms.inbound.domain.model.InboundItem

class InboundItemTestBuilder {

    private var inboundItemId: Long? = null
    private var inboundId: Long? = 1L
    private var productId: Long? = 1L
    private var zoneId: Long? = 1L
    private var expectedQuantity: Int = 10
    private var actualQuantity: Int? = null
    private var inspectionResult: InspectionResult = InspectionResult.PENDING

    companion object {
        fun inboundItem(): InboundItemTestBuilder = InboundItemTestBuilder()
    }

    fun inboundItemId(inboundItemId: Long?): InboundItemTestBuilder {
        this.inboundItemId = inboundItemId
        return this
    }

    fun inboundId(inboundId: Long?): InboundItemTestBuilder {
        this.inboundId = inboundId
        return this
    }

    fun productId(productId: Long?): InboundItemTestBuilder {
        this.productId = productId
        return this
    }

    fun zoneId(zoneId: Long?): InboundItemTestBuilder {
        this.zoneId = zoneId
        return this
    }

    fun expectedQuantity(expectedQuantity: Int): InboundItemTestBuilder {
        this.expectedQuantity = expectedQuantity
        return this
    }

    fun actualQuantity(actualQuantity: Int?): InboundItemTestBuilder {
        this.actualQuantity = actualQuantity
        return this
    }

    fun inspectionResult(inspectionResult: InspectionResult): InboundItemTestBuilder {
        this.inspectionResult = inspectionResult
        return this
    }

    fun build(): InboundItem {
        val id: Long? = inboundItemId
        if (id != null) {
            return InboundItem.reconstitute(
                inboundItemId = id,
                inboundId = requireNotNull(inboundId) { "inboundId는 재구성 시 필수입니다." },
                productId = requireNotNull(productId) { "productId는 재구성 시 필수입니다." },
                zoneId = requireNotNull(zoneId) { "zoneId는 재구성 시 필수입니다." },
                expectedQuantity = expectedQuantity,
                actualQuantity = actualQuantity,
                inspectionResult = inspectionResult
            )
        }
        return InboundItem.create(
            inboundId = inboundId,
            productId = productId,
            zoneId = zoneId,
            expectedQuantity = expectedQuantity
        )
    }
}
