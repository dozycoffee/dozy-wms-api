package com.dozycoffee.wms.outbound.domain.exception

import com.dozycoffee.wms.global.error.DomainException

class OutboundItemAlreadyPickedException : DomainException(OutboundItemErrorCode.ALREADY_PICKED)
