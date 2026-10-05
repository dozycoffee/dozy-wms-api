package com.dozycoffee.wms.warehouse.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class DuplicateLocationCodeException : ApplicationException(LocationErrorCode.DUPLICATE_LOCATION_CODE)
