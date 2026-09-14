package com.dozycoffee.wms.inbound.domain.exception

import com.dozycoffee.wms.global.error.DomainException

class InvalidActualQuantityException : DomainException(InboundItemErrorCode.INVALID_ACTUAL_QUANTITY)
