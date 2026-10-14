package com.dozycoffee.wms.outbound.domain.model

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.global.error.DomainValidator.requireNonNull
import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.outbound.domain.exception.OutboundAllocationErrorCode
import java.time.LocalDate

/** 확정된 예약이 한 출고 상품에 할당한 Lot 단위 수량. [inventoryId]는 inventory 서비스의 재고 행 ID다 */
class OutboundAllocation private constructor(
    val outboundAllocationId: Long?,
    val outboundItemId: Long,
    val inventoryId: Long,
    val lotNumber: String,
    val expirationDate: LocalDate?,
    quantity: Int,
    pickedQuantity: Int?
) : BaseEntity() {

    var quantity: Int = quantity
        private set

    var pickedQuantity: Int? = pickedQuantity
        private set

    companion object {

        fun create(
            outboundItemId: Long?,
            inventoryId: Long?,
            lotNumber: String?,
            expirationDate: LocalDate?,
            quantity: Int
        ): OutboundAllocation {
            val validOutboundItemId: Long = requireNonNull(outboundItemId, OutboundAllocationErrorCode.INVALID_OUTBOUND_ITEM_ID)
            val validInventoryId: Long = requireNonNull(inventoryId, OutboundAllocationErrorCode.INVALID_INVENTORY_ID)
            if (lotNumber.isNullOrBlank()) {
                throw InvalidDomainValueException(OutboundAllocationErrorCode.INVALID_LOT_NUMBER)
            }
            if (quantity <= 0) {
                throw InvalidDomainValueException(OutboundAllocationErrorCode.INVALID_QUANTITY)
            }
            return OutboundAllocation(null, validOutboundItemId, validInventoryId, lotNumber, expirationDate, quantity, null)
        }

        fun reconstitute(
            outboundAllocationId: Long,
            outboundItemId: Long,
            inventoryId: Long,
            lotNumber: String,
            expirationDate: LocalDate?,
            quantity: Int,
            pickedQuantity: Int?
        ): OutboundAllocation {
            return OutboundAllocation(outboundAllocationId, outboundItemId, inventoryId, lotNumber, expirationDate, quantity, pickedQuantity)
        }
    }

    /** 재할당으로 할당 수량이 바뀌면 피킹 수량은 다시 확보해야 하므로 비운다 */
    fun reallocate(quantity: Int) {
        if (quantity <= 0) {
            throw InvalidDomainValueException(OutboundAllocationErrorCode.INVALID_QUANTITY)
        }
        this.quantity = quantity
        this.pickedQuantity = null
    }

    /** 실제로 확보한 수량을 기록한다. 재고가 모자라면 할당 수량보다 적을 수 있다 */
    fun pick(pickedQuantity: Int) {
        if (pickedQuantity < 0 || pickedQuantity > quantity) {
            throw InvalidDomainValueException(OutboundAllocationErrorCode.INVALID_PICKED_QUANTITY)
        }
        this.pickedQuantity = pickedQuantity
    }
}
