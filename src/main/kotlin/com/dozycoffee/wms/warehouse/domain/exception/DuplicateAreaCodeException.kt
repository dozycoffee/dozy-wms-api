package com.dozycoffee.wms.warehouse.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class DuplicateAreaCodeException : ApplicationException(WorkAreaErrorCode.DUPLICATE_AREA_CODE)
