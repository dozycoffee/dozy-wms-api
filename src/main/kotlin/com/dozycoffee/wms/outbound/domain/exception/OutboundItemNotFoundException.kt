package com.dozycoffee.wms.outbound.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class OutboundItemNotFoundException : ApplicationException(OutboundItemErrorCode.OUTBOUND_ITEM_NOT_FOUND)
