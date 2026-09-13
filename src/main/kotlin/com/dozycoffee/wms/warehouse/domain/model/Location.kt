package com.dozycoffee.wms.warehouse.domain.model

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.global.error.DomainValidator.requireNonNull
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.exception.InactiveLocationException
import com.dozycoffee.wms.warehouse.domain.exception.InsufficientLocationCapacityException
import com.dozycoffee.wms.warehouse.domain.exception.InvalidLocationAmountException
import com.dozycoffee.wms.warehouse.domain.exception.LocationCapacityExceededException
import com.dozycoffee.wms.warehouse.domain.exception.LocationErrorCode
import com.dozycoffee.wms.warehouse.domain.valueobject.Capacity
import com.dozycoffee.wms.warehouse.domain.valueobject.LocationCode

class Location private constructor(
    val locationId: Long?,
    val zoneId: Long,
    val locationCode: LocationCode,
    val maxCapacity: Capacity,
    usedCapacity: Int,
    val locationStatus: AvailabilityStatus
) : BaseEntity() {

    var usedCapacity: Int = usedCapacity
        private set

    companion object {
        private const val INITIAL_USED_CAPACITY = 0

        fun create(
            zoneId: Long?,
            locationCode: String?,
            maxCapacity: Int,
            locationStatus: AvailabilityStatus?
        ): Location {
            val validZoneId: Long = requireNonNull(zoneId, LocationErrorCode.INVALID_ZONE_ID)
            val validLocationStatus: AvailabilityStatus =
                requireNonNull(locationStatus, LocationErrorCode.INVALID_LOCATION_STATUS)
            return Location(
                null,
                validZoneId,
                LocationCode.of(locationCode),
                Capacity(maxCapacity),
                INITIAL_USED_CAPACITY,
                validLocationStatus
            )
        }

        fun reconstitute(
            locationId: Long,
            zoneId: Long,
            locationCode: LocationCode,
            maxCapacity: Capacity,
            usedCapacity: Int,
            locationStatus: AvailabilityStatus
        ): Location {
            return Location(locationId, zoneId, locationCode, maxCapacity, usedCapacity, locationStatus)
        }
    }

    /** 재고 적재 */
    fun occupy(amount: Int) {
        validateAmount(amount)
        validateActive()
        validateCapacityNotExceeded(amount)
        usedCapacity += amount
    }

    /** 재고 반출 */
    fun release(amount: Int) {
        validateAmount(amount)
        validateActive()
        validateSufficientUsedCapacity(amount)
        usedCapacity -= amount
    }

    private fun validateAmount(amount: Int) {
        if (amount <= 0) {
            throw InvalidLocationAmountException()
        }
    }

    private fun validateActive() {
        if (locationStatus != AvailabilityStatus.AVAILABLE) {
            throw InactiveLocationException()
        }
    }

    private fun validateCapacityNotExceeded(amount: Int) {
        if (usedCapacity + amount > maxCapacity.value) {
            throw LocationCapacityExceededException()
        }
    }

    private fun validateSufficientUsedCapacity(amount: Int) {
        if (usedCapacity - amount < 0) {
            throw InsufficientLocationCapacityException()
        }
    }
}
