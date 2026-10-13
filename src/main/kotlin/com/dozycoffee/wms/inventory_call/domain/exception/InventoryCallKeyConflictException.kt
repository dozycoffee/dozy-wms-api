package com.dozycoffee.wms.inventory_call.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class InventoryCallKeyConflictException : ApplicationException(InventoryCallErrorCode.KEY_CONFLICT)
