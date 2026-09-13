package com.dozycoffee.wms.warehouse.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class WarehouseNotFoundException : ApplicationException(WarehouseErrorCode.WAREHOUSE_NOT_FOUND)
