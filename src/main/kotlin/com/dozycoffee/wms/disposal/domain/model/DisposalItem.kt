package com.dozycoffee.wms.disposal.domain.model

import com.dozycoffee.wms.disposal.domain.enumeration.DisposalReason
import com.dozycoffee.wms.disposal.domain.exception.DisposalItemErrorCode
import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.global.error.DomainValidator.requireNonNull
import com.dozycoffee.wms.global.error.InvalidDomainValueException

class DisposalItem private constructor(
    val disposalItemId: Long?,
    val disposalId: Long,
    val locationStockId: Long,
    val quantity: Int,
    val reason: DisposalReason
) : BaseEntity() {

    companion object {

        fun create(disposalId: Long?, locationStockId: Long?, quantity: Int, reason: DisposalReason): DisposalItem {
            val validDisposalId: Long = requireNonNull(disposalId, DisposalItemErrorCode.INVALID_DISPOSAL_ID)
            val validLocationStockId: Long = requireNonNull(locationStockId, DisposalItemErrorCode.INVALID_LOCATION_STOCK_ID)
            validateQuantity(quantity)
            return DisposalItem(
                disposalItemId = null,
                disposalId = validDisposalId,
                locationStockId = validLocationStockId,
                quantity = quantity,
                reason = reason
            )
        }

        fun reconstitute(
            disposalItemId: Long,
            disposalId: Long,
            locationStockId: Long,
            quantity: Int,
            reason: DisposalReason
        ): DisposalItem {
            return DisposalItem(disposalItemId, disposalId, locationStockId, quantity, reason)
        }

        private fun validateQuantity(quantity: Int) {
            if (quantity <= 0) {
                throw InvalidDomainValueException(DisposalItemErrorCode.INVALID_QUANTITY)
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DisposalItem) return false
        val id: Long? = disposalItemId
        return id != null && id == other.disposalItemId
    }

    override fun hashCode(): Int {
        return disposalItemId?.hashCode() ?: System.identityHashCode(this)
    }
}
