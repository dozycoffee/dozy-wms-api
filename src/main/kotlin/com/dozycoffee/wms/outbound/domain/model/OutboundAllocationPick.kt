package com.dozycoffee.wms.outbound.domain.model

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.global.error.DomainValidator.requireNonNull
import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.outbound.domain.exception.OutboundAllocationErrorCode

/** 출고 할당을 어느 위치 재고 행에서 몇 개 꺼낼지의 피킹 계획. 계획이 잡힌 수량은 그 행의 점유 수량(`held_quantity`)이다 */
class OutboundAllocationPick private constructor(
    val outboundAllocationPickId: Long?,
    val outboundAllocationId: Long,
    val locationStockId: Long,
    val quantity: Int
) : BaseEntity() {

    companion object {

        fun create(outboundAllocationId: Long?, locationStockId: Long?, quantity: Int): OutboundAllocationPick {
            val validAllocationId: Long = requireNonNull(outboundAllocationId, OutboundAllocationErrorCode.INVALID_OUTBOUND_ITEM_ID)
            val validLocationStockId: Long = requireNonNull(locationStockId, OutboundAllocationErrorCode.INVALID_INVENTORY_ID)
            if (quantity <= 0) {
                throw InvalidDomainValueException(OutboundAllocationErrorCode.INVALID_QUANTITY)
            }
            return OutboundAllocationPick(null, validAllocationId, validLocationStockId, quantity)
        }

        fun reconstitute(
            outboundAllocationPickId: Long,
            outboundAllocationId: Long,
            locationStockId: Long,
            quantity: Int
        ): OutboundAllocationPick {
            return OutboundAllocationPick(outboundAllocationPickId, outboundAllocationId, locationStockId, quantity)
        }
    }
}
