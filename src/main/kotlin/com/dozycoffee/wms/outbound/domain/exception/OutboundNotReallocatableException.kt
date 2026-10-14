package com.dozycoffee.wms.outbound.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class OutboundNotReallocatableException : ApplicationException(OutboundErrorCode.NOT_REALLOCATABLE)
