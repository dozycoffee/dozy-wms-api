package com.dozycoffee.wms.inbound.domain.exception

import com.dozycoffee.wms.global.error.DomainException

class ExpiredReceiptCannotBeNormalException : DomainException(InboundReceiptErrorCode.EXPIRED_CANNOT_BE_NORMAL)
