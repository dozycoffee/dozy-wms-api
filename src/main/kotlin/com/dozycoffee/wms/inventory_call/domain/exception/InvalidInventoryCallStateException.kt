package com.dozycoffee.wms.inventory_call.domain.exception

import com.dozycoffee.wms.global.error.DomainException

class InvalidInventoryCallStateException : DomainException(InventoryCallErrorCode.INVALID_STATE_TRANSITION)
