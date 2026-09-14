package com.dozycoffee.wms.inventory.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class DuplicateLotNumberException : ApplicationException(LotErrorCode.DUPLICATE_LOT_NUMBER)
