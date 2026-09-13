package com.dozycoffee.wms.warehouse.domain.exception

import com.dozycoffee.wms.global.error.DomainException

class InvalidWorkAreaAmountException : DomainException(WorkAreaErrorCode.INVALID_AMOUNT)
