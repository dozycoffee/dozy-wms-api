package com.dozycoffee.wms.inbound.domain.exception

import com.dozycoffee.wms.global.error.DomainException

class LotExpirationConflictException : DomainException(InboundReceiptErrorCode.LOT_EXPIRATION_CONFLICT)
