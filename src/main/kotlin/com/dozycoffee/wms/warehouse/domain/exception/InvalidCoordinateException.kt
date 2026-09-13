package com.dozycoffee.wms.warehouse.domain.exception

import com.dozycoffee.wms.global.error.DomainException

class InvalidCoordinateException : DomainException(WarehouseErrorCode.INVALID_COORDINATE)
