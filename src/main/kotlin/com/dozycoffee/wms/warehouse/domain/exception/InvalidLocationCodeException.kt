package com.dozycoffee.wms.warehouse.domain.exception

import com.dozycoffee.wms.global.error.DomainException

class InvalidLocationCodeException : DomainException(LocationErrorCode.INVALID_LOCATION_CODE)
