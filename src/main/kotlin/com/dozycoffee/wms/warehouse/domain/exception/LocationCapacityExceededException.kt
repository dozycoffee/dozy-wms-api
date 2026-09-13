package com.dozycoffee.wms.warehouse.domain.exception

import com.dozycoffee.wms.global.error.DomainException

class LocationCapacityExceededException : DomainException(LocationErrorCode.CAPACITY_EXCEEDED)
