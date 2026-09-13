package com.dozycoffee.wms.warehouse.domain.exception

import com.dozycoffee.wms.global.error.DomainException

class InactiveWorkAreaException : DomainException(WorkAreaErrorCode.INACTIVE_WORK_AREA)
