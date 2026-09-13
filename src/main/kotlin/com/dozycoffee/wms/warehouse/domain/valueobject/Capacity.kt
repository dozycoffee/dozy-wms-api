package com.dozycoffee.wms.warehouse.domain.valueobject

import com.dozycoffee.wms.warehouse.domain.exception.InvalidCapacityException

data class Capacity(val value: Int) {
    init {
        if (value < 0) {
            throw InvalidCapacityException()
        }
    }
}
