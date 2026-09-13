package com.dozycoffee.wms.warehouse.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class LocationNotFoundException : ApplicationException(LocationErrorCode.LOCATION_NOT_FOUND)
