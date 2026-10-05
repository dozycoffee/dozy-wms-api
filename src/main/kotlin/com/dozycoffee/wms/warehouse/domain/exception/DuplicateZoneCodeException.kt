package com.dozycoffee.wms.warehouse.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class DuplicateZoneCodeException : ApplicationException(ZoneErrorCode.DUPLICATE_ZONE_CODE)
