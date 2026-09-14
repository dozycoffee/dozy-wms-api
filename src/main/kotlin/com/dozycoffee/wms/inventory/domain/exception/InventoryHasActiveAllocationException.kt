package com.dozycoffee.wms.inventory.domain.exception

import com.dozycoffee.wms.global.error.DomainException

class InventoryHasActiveAllocationException : DomainException(InventoryErrorCode.HAS_ACTIVE_ALLOCATION)
