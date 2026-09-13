package com.dozycoffee.wms.warehouse.domain.exception

import com.dozycoffee.wms.global.error.DomainException

class InactiveLocationException : DomainException(LocationErrorCode.INACTIVE_LOCATION)
