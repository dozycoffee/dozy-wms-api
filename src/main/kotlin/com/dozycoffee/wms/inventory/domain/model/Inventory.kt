package com.dozycoffee.wms.inventory.domain.model

import com.dozycoffee.wms.global.common.SoftDeletableEntity
import com.dozycoffee.wms.global.error.DomainValidator.requireNonNull
import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.inventory.domain.enumeration.AllocationStatus
import com.dozycoffee.wms.inventory.domain.enumeration.QualityStatus
import com.dozycoffee.wms.inventory.domain.exception.InvalidInventoryStatusCombinationException
import com.dozycoffee.wms.inventory.domain.exception.InventoryErrorCode

class Inventory private constructor(
    val inventoryId: Long?,
    val productId: Long,
    val lotId: Long,
    val locationId: Long,
    quantity: Int,
    qualityStatus: QualityStatus,
    allocationStatus: AllocationStatus
) : SoftDeletableEntity() {

    var quantity: Int = quantity
        private set

    var qualityStatus: QualityStatus = qualityStatus
        private set

    var allocationStatus: AllocationStatus = allocationStatus
        private set

    companion object {

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
                qualityStatus = QualityStatus.NORMAL,
                allocationStatus = AllocationStatus.AVAILABLE
            )
        }

        fun reconstitute(
            inventoryId: Long,
            productId: Long,
            lotId: Long,
            locationId: Long,
            quantity: Int,
            qualityStatus: QualityStatus,
            allocationStatus: AllocationStatus
        ): Inventory {
            validateStatusCombination(qualityStatus, allocationStatus)
            return Inventory(
                inventoryId,
                productId,
                lotId,
                locationId,
                quantity,
                qualityStatus,
                allocationStatus
            )
        }

        private fun validateQuantity(quantity: Int) {
            if (quantity <= 0) {
                throw InvalidDomainValueException(InventoryErrorCode.INVALID_QUANTITY)
            }
        }

        private fun validateStatusCombination(qualityStatus: QualityStatus, allocationStatus: AllocationStatus) {
            if (qualityStatus != QualityStatus.NORMAL && allocationStatus == AllocationStatus.ALLOCATED) {
                throw InvalidInventoryStatusCombinationException()
            }
        }
    }

    /** 출고 등에서 재고를 할당한다 */
    fun allocate() {
        validateStatusCombination(qualityStatus, AllocationStatus.ALLOCATED)
        allocationStatus = AllocationStatus.ALLOCATED
    }

    /** 할당을 해제하고 가용 재고로 되돌린다 */
    fun release() {
        allocationStatus = AllocationStatus.AVAILABLE
    }

    /** 입고 검수 등에서 불량으로 판정한다 */
    fun markDefective() {
        validateStatusCombination(QualityStatus.DEFECTIVE, allocationStatus)
        qualityStatus = QualityStatus.DEFECTIVE
    }

    /** 유통기한 경과 등으로 폐기 예정 처리한다 */
    fun markDisposalScheduled() {
        validateStatusCombination(QualityStatus.DISPOSAL_SCHEDULED, allocationStatus)
        qualityStatus = QualityStatus.DISPOSAL_SCHEDULED
    }

    fun delete(actor: String) {
        softDelete(actor)
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
