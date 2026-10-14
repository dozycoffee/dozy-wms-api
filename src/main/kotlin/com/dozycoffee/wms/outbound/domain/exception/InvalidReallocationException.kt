package com.dozycoffee.wms.outbound.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class InvalidReallocationException : ApplicationException(OutboundErrorCode.INVALID_REALLOCATION)
