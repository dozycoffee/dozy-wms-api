package com.dozycoffee.wms.inventory.domain.exception

import com.dozycoffee.wms.global.error.DomainException

class InvalidInventoryStatusCombinationException : DomainException(InventoryErrorCode.INVALID_STATUS_COMBINATION)
