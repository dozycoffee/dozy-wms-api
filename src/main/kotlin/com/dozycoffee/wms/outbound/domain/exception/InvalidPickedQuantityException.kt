package com.dozycoffee.wms.outbound.domain.exception

import com.dozycoffee.wms.global.error.DomainException

class InvalidPickedQuantityException : DomainException(OutboundItemErrorCode.INVALID_PICKED_QUANTITY)
