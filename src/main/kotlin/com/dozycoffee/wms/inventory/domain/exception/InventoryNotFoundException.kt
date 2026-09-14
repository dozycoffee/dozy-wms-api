package com.dozycoffee.wms.inventory.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class InventoryNotFoundException : ApplicationException(InventoryErrorCode.INVENTORY_NOT_FOUND)
