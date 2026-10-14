package com.dozycoffee.wms.outbound.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class OutboundNotCancelableException : ApplicationException(OutboundErrorCode.NOT_CANCELABLE)
