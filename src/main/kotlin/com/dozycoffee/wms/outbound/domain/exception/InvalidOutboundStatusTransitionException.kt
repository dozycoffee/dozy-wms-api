package com.dozycoffee.wms.outbound.domain.exception

import com.dozycoffee.wms.global.error.DomainException

class InvalidOutboundStatusTransitionException : DomainException(OutboundErrorCode.INVALID_STATUS_TRANSITION)
