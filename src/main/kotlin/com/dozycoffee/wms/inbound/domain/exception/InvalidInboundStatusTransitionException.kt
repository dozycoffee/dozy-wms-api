package com.dozycoffee.wms.inbound.domain.exception

import com.dozycoffee.wms.global.error.DomainException

class InvalidInboundStatusTransitionException : DomainException(InboundErrorCode.INVALID_STATUS_TRANSITION)
