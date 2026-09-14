package com.dozycoffee.wms.inventory.domain.exception

import com.dozycoffee.wms.global.error.DomainException

class InsufficientAvailableQuantityException : DomainException(InventoryErrorCode.INSUFFICIENT_AVAILABLE_QUANTITY)
