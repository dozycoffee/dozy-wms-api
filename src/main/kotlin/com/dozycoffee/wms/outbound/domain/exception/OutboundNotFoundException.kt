package com.dozycoffee.wms.outbound.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class OutboundNotFoundException : ApplicationException(OutboundErrorCode.OUTBOUND_NOT_FOUND)
