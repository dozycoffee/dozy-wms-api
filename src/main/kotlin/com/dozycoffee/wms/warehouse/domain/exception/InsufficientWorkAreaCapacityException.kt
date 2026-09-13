package com.dozycoffee.wms.warehouse.domain.exception

import com.dozycoffee.wms.global.error.DomainException

class InsufficientWorkAreaCapacityException : DomainException(WorkAreaErrorCode.INSUFFICIENT_USED_CAPACITY)
