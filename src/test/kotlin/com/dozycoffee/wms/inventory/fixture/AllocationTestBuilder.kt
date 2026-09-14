package com.dozycoffee.wms.inventory.fixture

import com.dozycoffee.wms.inventory.domain.enumeration.AllocationReferenceType
import com.dozycoffee.wms.inventory.domain.enumeration.AllocationStatus
import com.dozycoffee.wms.inventory.domain.model.Allocation

class AllocationTestBuilder {

    private var allocationId: Long? = null
    private var inventoryId: Long? = 1L
    private var referenceType: AllocationReferenceType? = AllocationReferenceType.OUTBOUND
    private var referenceId: Long? = 1L
    private var quantity: Int = 10
    private var status: AllocationStatus = AllocationStatus.HELD

    companion object {
        fun allocation(): AllocationTestBuilder = AllocationTestBuilder()
    }

    fun allocationId(allocationId: Long?): AllocationTestBuilder {
        this.allocationId = allocationId
        return this
    }

    fun inventoryId(inventoryId: Long?): AllocationTestBuilder {
        this.inventoryId = inventoryId
        return this
    }

    fun referenceType(referenceType: AllocationReferenceType?): AllocationTestBuilder {
        this.referenceType = referenceType
        return this
    }

    fun referenceId(referenceId: Long?): AllocationTestBuilder {
        this.referenceId = referenceId
        return this
    }

    fun quantity(quantity: Int): AllocationTestBuilder {
        this.quantity = quantity
        return this
    }

    fun status(status: AllocationStatus): AllocationTestBuilder {
        this.status = status
        return this
    }

    fun build(): Allocation {
        val id: Long? = allocationId
        if (id != null) {
            return Allocation.reconstitute(
                allocationId = id,
                inventoryId = requireNotNull(inventoryId) { "inventoryId는 재구성 시 필수입니다." },
                referenceType = requireNotNull(referenceType) { "referenceType은 재구성 시 필수입니다." },
                referenceId = requireNotNull(referenceId) { "referenceId는 재구성 시 필수입니다." },
                quantity = quantity,
                status = status
            )
        }
        return Allocation.create(
            inventoryId = inventoryId,
            referenceType = referenceType,
            referenceId = referenceId,
            quantity = quantity
        )
    }
}
