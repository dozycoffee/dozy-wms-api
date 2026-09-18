package com.dozycoffee.wms.inventory.domain.model

import com.dozycoffee.wms.global.common.SoftDeletableEntity
import com.dozycoffee.wms.global.error.DomainValidator.requireNonNull
import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.inventory.domain.enumeration.QualityStatus
import com.dozycoffee.wms.inventory.domain.exception.AdjustedQuantityBelowAllocatedException
import com.dozycoffee.wms.inventory.domain.exception.InsufficientAvailableQuantityException
import com.dozycoffee.wms.inventory.domain.exception.InsufficientHeldQuantityException
import com.dozycoffee.wms.inventory.domain.exception.InvalidAdjustedQuantityException
import com.dozycoffee.wms.inventory.domain.exception.InventoryErrorCode
import com.dozycoffee.wms.inventory.domain.exception.InventoryHasActiveAllocationException
import com.dozycoffee.wms.inventory.domain.exception.InventoryNotAllocatableException
import com.dozycoffee.wms.inventory.domain.exception.InvalidInventoryAmountException

class Inventory private constructor(
    val inventoryId: Long?,
    val productId: Long,
    val lotId: Long,
    val locationId: Long,
    quantity: Int,
    allocatedQuantity: Int,
    qualityStatus: QualityStatus
) : SoftDeletableEntity() {

    var quantity: Int = quantity
        private set

    var allocatedQuantity: Int = allocatedQuantity
        private set

    var qualityStatus: QualityStatus = qualityStatus
        private set

    val availableQuantity: Int
        get() = quantity - allocatedQuantity

    companion object {
        private const val INITIAL_ALLOCATED_QUANTITY = 0

        fun create(
            productId: Long?,
            lotId: Long?,
            locationId: Long?,
            quantity: Int
        ): Inventory {
            val validProductId: Long = requireNonNull(productId, InventoryErrorCode.INVALID_PRODUCT_ID)
            val validLotId: Long = requireNonNull(lotId, InventoryErrorCode.INVALID_LOT_ID)
            val validLocationId: Long = requireNonNull(locationId, InventoryErrorCode.INVALID_LOCATION_ID)
            validateQuantity(quantity)
            return Inventory(
                inventoryId = null,
                productId = validProductId,
                lotId = validLotId,
                locationId = validLocationId,
                quantity = quantity,
                allocatedQuantity = INITIAL_ALLOCATED_QUANTITY,
                qualityStatus = QualityStatus.NORMAL
            )
        }

        fun reconstitute(
            inventoryId: Long,
            productId: Long,
            lotId: Long,
            locationId: Long,
            quantity: Int,
            allocatedQuantity: Int,
            qualityStatus: QualityStatus
        ): Inventory {
            return Inventory(
                inventoryId,
                productId,
                lotId,
                locationId,
                quantity,
                allocatedQuantity,
                qualityStatus
            )
        }

        private fun validateQuantity(quantity: Int) {
            if (quantity <= 0) {
                throw InvalidDomainValueException(InventoryErrorCode.INVALID_QUANTITY)
            }
        }
    }

    /** Allocation이 HELD로 생성될 때 그만큼 가용 수량에서 점유 처리한다 */
    fun hold(amount: Int) {
        validateAmount(amount)
        validateAllocatable()
        validateSufficientAvailable(amount)
        allocatedQuantity += amount
    }

    /** Allocation이 RELEASED로 전환될 때 점유를 해제하고 가용 수량으로 되돌린다 */
    fun releaseHold(amount: Int) {
        validateAmount(amount)
        validateSufficientHeld(amount)
        allocatedQuantity -= amount
    }

    /** Allocation이 FULFILLED로 전환될 때 점유 수량만큼 실제 재고에서 차감한다 */
    fun fulfillHold(amount: Int) {
        validateAmount(amount)
        validateSufficientHeld(amount)
        quantity -= amount
        allocatedQuantity -= amount
    }

    /** 입고 검수 등에서 불량으로 판정한다 */
    fun markDefective() {
        validateNoActiveAllocation()
        qualityStatus = QualityStatus.DEFECTIVE
    }

    /** 유통기한 경과 등으로 폐기 예정 처리한다 */
    fun markDisposalScheduled() {
        validateNoActiveAllocation()
        qualityStatus = QualityStatus.DISPOSAL_SCHEDULED
    }

    fun delete(actor: String) {
        softDelete(actor)
    }

    /** 재고 실사 확정 시 실측 수량으로 재고를 교정한다 — 점유된 수량보다 적게 조정할 수 없다(ADR-0009) */
    fun adjust(newQuantity: Int) {
        if (newQuantity < 0) {
            throw InvalidAdjustedQuantityException()
        }
        if (newQuantity < allocatedQuantity) {
            throw AdjustedQuantityBelowAllocatedException()
        }
        quantity = newQuantity
    }

    private fun validateAmount(amount: Int) {
        if (amount <= 0) {
            throw InvalidInventoryAmountException()
        }
    }

    private fun validateAllocatable() {
        if (qualityStatus != QualityStatus.NORMAL) {
            throw InventoryNotAllocatableException()
        }
    }

    private fun validateSufficientAvailable(amount: Int) {
        if (amount > availableQuantity) {
            throw InsufficientAvailableQuantityException()
        }
    }

    private fun validateSufficientHeld(amount: Int) {
        if (amount > allocatedQuantity) {
            throw InsufficientHeldQuantityException()
        }
    }

    private fun validateNoActiveAllocation() {
        if (allocatedQuantity > 0) {
            throw InventoryHasActiveAllocationException()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Inventory) return false
        val id: Long? = inventoryId
        return id != null && id == other.inventoryId
    }

    override fun hashCode(): Int {
        return inventoryId?.hashCode() ?: System.identityHashCode(this)
    }
}
