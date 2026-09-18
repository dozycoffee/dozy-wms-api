package com.dozycoffee.wms.inventory.domain.exception

import com.dozycoffee.wms.global.error.DomainException

class InvalidAdjustedQuantityException : DomainException(InventoryErrorCode.INVALID_ADJUSTED_QUANTITY)
