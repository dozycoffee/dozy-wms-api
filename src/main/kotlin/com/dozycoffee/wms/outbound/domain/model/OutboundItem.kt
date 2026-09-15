package com.dozycoffee.wms.outbound.domain.model

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.global.error.DomainValidator.requireNonNull
import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.outbound.domain.exception.InvalidPickedQuantityException
import com.dozycoffee.wms.outbound.domain.exception.OutboundItemAlreadyPickedException
import com.dozycoffee.wms.outbound.domain.exception.OutboundItemErrorCode

class OutboundItem private constructor(
    val outboundItemId: Long?,
    val outboundId: Long,
    val productId: Long,
    val requestedQuantity: Int,
    pickedQuantity: Int?
) : BaseEntity() {

    var pickedQuantity: Int? = pickedQuantity
        private set

    /** 요청 수량 대비 실제 피킹 수량의 부족분 — 피킹 전에는 null */
    val shortageQuantity: Int?
        get() = pickedQuantity?.let { requestedQuantity - it }

    companion object {

        fun create(outboundId: Long?, productId: Long?, requestedQuantity: Int): OutboundItem {
            val validOutboundId: Long = requireNonNull(outboundId, OutboundItemErrorCode.INVALID_OUTBOUND_ID)
            val validProductId: Long = requireNonNull(productId, OutboundItemErrorCode.INVALID_PRODUCT_ID)
            validateRequestedQuantity(requestedQuantity)
            return OutboundItem(
                outboundItemId = null,
                outboundId = validOutboundId,
                productId = validProductId,
                requestedQuantity = requestedQuantity,
                pickedQuantity = null
            )
        }

        fun reconstitute(
            outboundItemId: Long,
            outboundId: Long,
            productId: Long,
            requestedQuantity: Int,
            pickedQuantity: Int?
        ): OutboundItem {
            return OutboundItem(outboundItemId, outboundId, productId, requestedQuantity, pickedQuantity)
        }

        private fun validateRequestedQuantity(requestedQuantity: Int) {
            if (requestedQuantity <= 0) {
                throw InvalidDomainValueException(OutboundItemErrorCode.INVALID_REQUESTED_QUANTITY)
            }
        }
    }

    /** FIFO 피킹으로 확보한 실제 수량을 기록한다 — 재고 부족 시 요청 수량보다 적을 수 있다 */
    fun pick(pickedQuantity: Int) {
        validateNotAlreadyPicked()
        validatePickedQuantity(pickedQuantity)
        this.pickedQuantity = pickedQuantity
    }

    private fun validateNotAlreadyPicked() {
        if (pickedQuantity != null) {
            throw OutboundItemAlreadyPickedException()
        }
    }

    private fun validatePickedQuantity(pickedQuantity: Int) {
        if (pickedQuantity < 0 || pickedQuantity > requestedQuantity) {
            throw InvalidPickedQuantityException()
        }
    }
}
