package com.dozycoffee.wms.inventory.domain.model

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.global.error.DomainValidator.requireNonNull
import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.inventory.domain.enumeration.AllocationReferenceType
import com.dozycoffee.wms.inventory.domain.enumeration.AllocationStatus
import com.dozycoffee.wms.inventory.domain.exception.AllocationErrorCode
import com.dozycoffee.wms.inventory.domain.exception.InvalidAllocationStatusTransitionException

class Allocation private constructor(
    val allocationId: Long?,
    val inventoryId: Long,
    val referenceType: AllocationReferenceType,
    val referenceId: Long,
    val quantity: Int,
    status: AllocationStatus
) : BaseEntity() {

    var status: AllocationStatus = status
        private set

    companion object {

        fun create(
            inventoryId: Long?,
            referenceType: AllocationReferenceType?,
            referenceId: Long?,
            quantity: Int
        ): Allocation {
            val validInventoryId: Long = requireNonNull(inventoryId, AllocationErrorCode.INVALID_INVENTORY_ID)
            val validReferenceType: AllocationReferenceType =
                requireNonNull(referenceType, AllocationErrorCode.INVALID_REFERENCE_TYPE)
            val validReferenceId: Long = requireNonNull(referenceId, AllocationErrorCode.INVALID_REFERENCE_ID)
            validateQuantity(quantity)
            return Allocation(
                allocationId = null,
                inventoryId = validInventoryId,
                referenceType = validReferenceType,
                referenceId = validReferenceId,
                quantity = quantity,
                status = AllocationStatus.HELD
            )
        }

        fun reconstitute(
            allocationId: Long,
            inventoryId: Long,
            referenceType: AllocationReferenceType,
            referenceId: Long,
            quantity: Int,
            status: AllocationStatus
        ): Allocation {
            return Allocation(allocationId, inventoryId, referenceType, referenceId, quantity, status)
        }

        private fun validateQuantity(quantity: Int) {
            if (quantity <= 0) {
                throw InvalidDomainValueException(AllocationErrorCode.INVALID_QUANTITY)
            }
        }
    }

    /** 점유를 취소한다 — 해당 수량은 Inventory.releaseHold()를 통해 다시 가용 상태로 돌아간다 */
    fun release() {
        transitionTo(AllocationStatus.RELEASED)
    }

    /** 점유를 실제 이행(출고 완료 등)으로 확정한다 — Inventory.fulfillHold()와 함께 호출된다 */
    fun fulfill() {
        transitionTo(AllocationStatus.FULFILLED)
    }

    private fun transitionTo(target: AllocationStatus) {
        if (!status.canTransitionTo(target)) {
            throw InvalidAllocationStatusTransitionException()
        }
        status = target
    }
}
