package com.dozycoffee.wms.outbound.fixture

import com.dozycoffee.wms.outbound.domain.model.OutboundItem

class OutboundItemTestBuilder {

    private var outboundItemId: Long? = null
    private var outboundId: Long? = 1L
    private var productId: Long? = 1L
    private var requestedQuantity: Int = 10
    private var pickedQuantity: Int? = null

    companion object {
        fun outboundItem(): OutboundItemTestBuilder = OutboundItemTestBuilder()
    }

    fun outboundItemId(outboundItemId: Long?): OutboundItemTestBuilder {
        this.outboundItemId = outboundItemId
        return this
    }

    fun outboundId(outboundId: Long?): OutboundItemTestBuilder {
        this.outboundId = outboundId
        return this
    }

    fun productId(productId: Long?): OutboundItemTestBuilder {
        this.productId = productId
        return this
    }

    fun requestedQuantity(requestedQuantity: Int): OutboundItemTestBuilder {
        this.requestedQuantity = requestedQuantity
        return this
    }

    fun pickedQuantity(pickedQuantity: Int?): OutboundItemTestBuilder {
        this.pickedQuantity = pickedQuantity
        return this
    }

    fun build(): OutboundItem {
        val id: Long? = outboundItemId
        if (id != null) {
            return OutboundItem.reconstitute(
                outboundItemId = id,
                outboundId = requireNotNull(outboundId) { "outboundId는 재구성 시 필수입니다." },
                productId = requireNotNull(productId) { "productId는 재구성 시 필수입니다." },
                requestedQuantity = requestedQuantity,
                pickedQuantity = pickedQuantity
            )
        }
        return OutboundItem.create(
            outboundId = outboundId,
            productId = productId,
            requestedQuantity = requestedQuantity
        )
    }
}
