package com.dozycoffee.wms.return_request.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class InsufficientZoneCapacityException : ApplicationException(ReturnRequestErrorCode.INSUFFICIENT_ZONE_CAPACITY)
