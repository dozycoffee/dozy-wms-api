package com.dozycoffee.wms.inbound.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class NotAllItemsInspectedException : ApplicationException(InboundErrorCode.NOT_ALL_ITEMS_INSPECTED)
