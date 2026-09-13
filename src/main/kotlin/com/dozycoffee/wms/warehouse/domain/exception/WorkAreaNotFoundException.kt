package com.dozycoffee.wms.warehouse.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class WorkAreaNotFoundException : ApplicationException(WorkAreaErrorCode.WORK_AREA_NOT_FOUND)
