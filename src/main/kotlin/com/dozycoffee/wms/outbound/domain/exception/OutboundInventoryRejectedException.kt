package com.dozycoffee.wms.outbound.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class OutboundInventoryRejectedException : ApplicationException(OutboundErrorCode.INVENTORY_REJECTED)
