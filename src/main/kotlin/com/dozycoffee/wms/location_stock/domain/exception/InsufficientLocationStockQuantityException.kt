package com.dozycoffee.wms.location_stock.domain.exception

import com.dozycoffee.wms.global.error.DomainException

class InsufficientLocationStockQuantityException : DomainException(LocationStockErrorCode.INSUFFICIENT_QUANTITY)
