package com.dozycoffee.wms.warehouse.domain.exception

import com.dozycoffee.wms.global.error.DomainException

class InvalidCapacityException : DomainException(WarehouseErrorCode.INVALID_CAPACITY)
