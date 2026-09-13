package com.dozycoffee.wms.warehouse.domain.valueobject

import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.warehouse.domain.exception.WarehouseErrorCode

@ConsistentCopyVisibility
data class Address private constructor(val value: String) {
    companion object {
        fun of(value: String?): Address {
            if (value.isNullOrBlank()) {
                throw InvalidDomainValueException(WarehouseErrorCode.INVALID_ADDRESS)
            }
            return Address(value)
        }
    }
}
