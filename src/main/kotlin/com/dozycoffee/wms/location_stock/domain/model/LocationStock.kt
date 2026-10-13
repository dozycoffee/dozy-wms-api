package com.dozycoffee.wms.location_stock.domain.model

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.global.error.DomainValidator.requireNonNull
import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.location_stock.domain.enumeration.StockQualityStatus
import com.dozycoffee.wms.location_stock.domain.exception.InsufficientLocationStockQuantityException
import com.dozycoffee.wms.location_stock.domain.exception.LocationStockErrorCode

/** 수량이 어느 Location에 있는지 기록하는 물리 위치 장부의 한 행 — 수량의 진실은 inventory 서비스다(ADR-0017, ADR-0018) */
class LocationStock private constructor(
    val locationStockId: Long?,
    val locationId: Long,
    val productId: Long,
    val lotNumber: String,
    val qualityStatus: StockQualityStatus,
    quantity: Int
) : BaseEntity() {

    var quantity: Int = quantity
        private set

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
            return LocationStock(null, validLocationId, validProductId, validLotNumber, qualityStatus, quantity)
        }

        fun reconstitute(
            locationStockId: Long,
            locationId: Long,
            productId: Long,
            lotNumber: String,
            qualityStatus: StockQualityStatus,
            quantity: Int
        ): LocationStock {
            return LocationStock(locationStockId, locationId, productId, lotNumber, qualityStatus, quantity)
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

    fun decrease(amount: Int) {
        validateAmount(amount)
        if (amount > quantity) {
            throw InsufficientLocationStockQuantityException()
        }
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
