package com.dozycoffee.wms.inventory.domain.exception

import com.dozycoffee.wms.global.error.DomainException

class AdjustedQuantityBelowAllocatedException : DomainException(InventoryErrorCode.ADJUSTED_QUANTITY_BELOW_ALLOCATED)
