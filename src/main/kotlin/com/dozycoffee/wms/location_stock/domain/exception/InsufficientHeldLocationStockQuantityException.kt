package com.dozycoffee.wms.location_stock.domain.exception

import com.dozycoffee.wms.global.error.DomainException

class InsufficientHeldLocationStockQuantityException : DomainException(LocationStockErrorCode.INSUFFICIENT_HELD_QUANTITY)
