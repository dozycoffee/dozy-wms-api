package com.dozycoffee.wms.location_stock.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class DuplicateLocationStockException : ApplicationException(LocationStockErrorCode.DUPLICATE_LOCATION_STOCK)
