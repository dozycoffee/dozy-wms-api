package com.dozycoffee.wms.disposal.fixture

import com.dozycoffee.wms.disposal.domain.enumeration.DisposalReason
import com.dozycoffee.wms.disposal.domain.model.DisposalItem

class DisposalItemTestBuilder {

    private var disposalItemId: Long? = null
    private var disposalId: Long? = 1L
    private var inventoryId: Long? = 1L
    private var quantity: Int = 10
    private var reason: DisposalReason = DisposalReason.EXPIRED

    companion object {
        fun disposalItem(): DisposalItemTestBuilder = DisposalItemTestBuilder()
    }

    fun disposalItemId(disposalItemId: Long?): DisposalItemTestBuilder {
        this.disposalItemId = disposalItemId
        return this
    }

    fun disposalId(disposalId: Long?): DisposalItemTestBuilder {
        this.disposalId = disposalId
        return this
    }

    fun inventoryId(inventoryId: Long?): DisposalItemTestBuilder {
        this.inventoryId = inventoryId
        return this
    }

    fun quantity(quantity: Int): DisposalItemTestBuilder {
        this.quantity = quantity
        return this
    }

    fun reason(reason: DisposalReason): DisposalItemTestBuilder {
        this.reason = reason
        return this
    }

    fun build(): DisposalItem {
        val id: Long? = disposalItemId
        if (id != null) {
            return DisposalItem.reconstitute(
                disposalItemId = id,
                disposalId = requireNotNull(disposalId) { "disposalId는 재구성 시 필수입니다." },
                inventoryId = requireNotNull(inventoryId) { "inventoryId는 재구성 시 필수입니다." },
                quantity = quantity,
                reason = reason
            )
        }
        return DisposalItem.create(
            disposalId = disposalId,
            inventoryId = inventoryId,
            quantity = quantity,
            reason = reason
        )
    }
}
