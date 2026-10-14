package com.dozycoffee.wms.location_stock.domain.model

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.global.error.DomainValidator.requireNonNull
import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.location_stock.domain.enumeration.StockQualityStatus
import com.dozycoffee.wms.location_stock.domain.exception.InsufficientHeldLocationStockQuantityException
import com.dozycoffee.wms.location_stock.domain.exception.InsufficientLocationStockQuantityException
import com.dozycoffee.wms.location_stock.domain.exception.LocationStockErrorCode

/** 수량이 어느 Location에 있는지 기록하는 물리 위치 장부의 한 행 — 수량의 진실은 inventory 서비스다(ADR-0017, ADR-0018) */
class LocationStock private constructor(
    val locationStockId: Long?,
    val locationId: Long,
    val productId: Long,
    val lotNumber: String,
    val qualityStatus: StockQualityStatus,
    quantity: Int,
    heldQuantity: Int
) : BaseEntity() {

    var quantity: Int = quantity
        private set

    /** 출고 피킹 계획이 잡아 둔 수량. 다른 출고나 감소가 쓸 수 없다 */
    var heldQuantity: Int = heldQuantity
        private set

    val availableQuantity: Int
        get() = quantity - heldQuantity

    companion object {
        private const val MAX_LOT_NUMBER_LENGTH = 50

        fun create(
            locationId: Long?,
            productId: Long?,
            lotNumber: String?,
            qualityStatus: StockQualityStatus,
            quantity: Int
        ): LocationStock {
            val validLocationId: Long = requireNonNull(locationId, LocationStockErrorCode.INVALID_LOCATION_ID)
            val validProductId: Long = requireNonNull(productId, LocationStockErrorCode.INVALID_PRODUCT_ID)
            val validLotNumber: String = validateLotNumber(lotNumber)
            validateAmount(quantity)
            return LocationStock(null, validLocationId, validProductId, validLotNumber, qualityStatus, quantity, 0)
        }

        fun reconstitute(
            locationStockId: Long,
            locationId: Long,
            productId: Long,
            lotNumber: String,
            qualityStatus: StockQualityStatus,
            quantity: Int,
            heldQuantity: Int = 0
        ): LocationStock {
            return LocationStock(locationStockId, locationId, productId, lotNumber, qualityStatus, quantity, heldQuantity)
        }

        private fun validateLotNumber(lotNumber: String?): String {
            if (lotNumber.isNullOrBlank() || lotNumber.length > MAX_LOT_NUMBER_LENGTH) {
                throw InvalidDomainValueException(LocationStockErrorCode.INVALID_LOT_NUMBER)
            }
            return lotNumber
        }

        private fun validateAmount(amount: Int) {
            if (amount <= 0) {
                throw InvalidDomainValueException(LocationStockErrorCode.INVALID_AMOUNT)
            }
        }
    }

    fun increase(amount: Int) {
        validateAmount(amount)
        quantity += amount
    }

    /** 점유되지 않은 수량 안에서만 줄일 수 있다 */
    fun decrease(amount: Int) {
        validateAmount(amount)
        if (amount > availableQuantity) {
            throw InsufficientLocationStockQuantityException()
        }
        quantity -= amount
    }

    fun hold(amount: Int) {
        validateAmount(amount)
        if (amount > availableQuantity) {
            throw InsufficientLocationStockQuantityException()
        }
        heldQuantity += amount
    }

    fun releaseHold(amount: Int) {
        validateAmount(amount)
        if (amount > heldQuantity) {
            throw InsufficientHeldLocationStockQuantityException()
        }
        heldQuantity -= amount
    }

    /** 점유한 수량을 실제로 내보낸다. 총 수량과 점유 수량이 함께 준다 */
    fun consume(amount: Int) {
        validateAmount(amount)
        if (amount > heldQuantity) {
            throw InsufficientHeldLocationStockQuantityException()
        }
        heldQuantity -= amount
        quantity -= amount
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LocationStock) return false
        val id: Long? = locationStockId
        return id != null && id == other.locationStockId
    }

    override fun hashCode(): Int {
        return locationStockId?.hashCode() ?: System.identityHashCode(this)
    }
}
