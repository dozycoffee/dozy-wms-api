package com.dozycoffee.wms.warehouse.domain.exception

import com.dozycoffee.wms.global.error.DomainException

class WorkAreaCapacityExceededException : DomainException(WorkAreaErrorCode.CAPACITY_EXCEEDED)
