package com.dozycoffee.wms.inventory.domain.exception

import com.dozycoffee.wms.global.error.DomainException

class InsufficientHeldQuantityException : DomainException(InventoryErrorCode.INSUFFICIENT_HELD_QUANTITY)
