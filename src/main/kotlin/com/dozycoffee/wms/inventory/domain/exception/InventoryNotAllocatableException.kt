package com.dozycoffee.wms.inventory.domain.exception

import com.dozycoffee.wms.global.error.DomainException

class InventoryNotAllocatableException : DomainException(InventoryErrorCode.NOT_ALLOCATABLE)
