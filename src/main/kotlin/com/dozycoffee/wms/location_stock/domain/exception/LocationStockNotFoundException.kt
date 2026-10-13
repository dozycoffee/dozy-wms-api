package com.dozycoffee.wms.location_stock.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class LocationStockNotFoundException : ApplicationException(LocationStockErrorCode.LOCATION_STOCK_NOT_FOUND)
