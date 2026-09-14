package com.dozycoffee.wms.inbound.domain.exception

import com.dozycoffee.wms.global.error.DomainException

class InboundItemAlreadyInspectedException : DomainException(InboundItemErrorCode.ALREADY_INSPECTED)
