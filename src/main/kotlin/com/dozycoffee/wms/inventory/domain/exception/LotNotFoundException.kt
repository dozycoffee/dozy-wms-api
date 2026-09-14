package com.dozycoffee.wms.inventory.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class LotNotFoundException : ApplicationException(LotErrorCode.LOT_NOT_FOUND)
