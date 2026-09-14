package com.dozycoffee.wms.inbound.domain.exception

import com.dozycoffee.wms.global.error.DomainException

class InvalidInspectionResultException : DomainException(InboundItemErrorCode.INVALID_INSPECTION_RESULT)
