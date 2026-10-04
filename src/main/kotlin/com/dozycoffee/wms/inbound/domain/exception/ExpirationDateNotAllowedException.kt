package com.dozycoffee.wms.inbound.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class ExpirationDateNotAllowedException : ApplicationException(InboundReceiptErrorCode.EXPIRATION_DATE_NOT_ALLOWED)
