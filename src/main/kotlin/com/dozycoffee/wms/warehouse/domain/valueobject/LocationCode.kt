package com.dozycoffee.wms.warehouse.domain.valueobject

import com.dozycoffee.wms.warehouse.domain.exception.InvalidLocationCodeException

@ConsistentCopyVisibility
data class LocationCode private constructor(val value: String) {
    companion object {
        private val LOCATION_CODE_PATTERN = Regex("^[A-Z]-\\d{2}$")

        fun of(value: String?): LocationCode {
            if (value == null || !LOCATION_CODE_PATTERN.matches(value)) {
                throw InvalidLocationCodeException()
            }
            return LocationCode(value)
        }
    }
}
