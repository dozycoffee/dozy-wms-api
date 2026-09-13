package com.dozycoffee.wms.warehouse.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class ZoneNotFoundException : ApplicationException(ZoneErrorCode.ZONE_NOT_FOUND)
