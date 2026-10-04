package com.dozycoffee.wms.inbound.domain.exception

import com.dozycoffee.wms.global.error.DomainException

class InboundOverReceivedException : DomainException(InboundReceiptErrorCode.OVER_RECEIVED)
