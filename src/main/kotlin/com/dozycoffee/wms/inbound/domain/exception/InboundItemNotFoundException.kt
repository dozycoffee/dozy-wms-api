package com.dozycoffee.wms.inbound.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class InboundItemNotFoundException : ApplicationException(InboundItemErrorCode.INBOUND_ITEM_NOT_FOUND)
