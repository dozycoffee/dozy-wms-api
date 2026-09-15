package com.dozycoffee.wms.inbound.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class InsufficientZoneCapacityException : ApplicationException(InboundErrorCode.INSUFFICIENT_ZONE_CAPACITY)
