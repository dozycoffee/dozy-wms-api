package com.dozycoffee.wms.inbound.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class InboundNotFoundException : ApplicationException(InboundErrorCode.INBOUND_NOT_FOUND)
