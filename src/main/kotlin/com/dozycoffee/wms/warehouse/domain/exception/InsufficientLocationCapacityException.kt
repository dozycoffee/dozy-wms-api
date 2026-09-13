package com.dozycoffee.wms.warehouse.domain.exception

import com.dozycoffee.wms.global.error.DomainException

class InsufficientLocationCapacityException : DomainException(LocationErrorCode.INSUFFICIENT_USED_CAPACITY)
