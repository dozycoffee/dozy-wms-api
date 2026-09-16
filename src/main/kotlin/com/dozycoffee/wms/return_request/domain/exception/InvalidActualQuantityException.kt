package com.dozycoffee.wms.return_request.domain.exception

import com.dozycoffee.wms.global.error.DomainException

class InvalidActualQuantityException : DomainException(ReturnItemErrorCode.INVALID_ACTUAL_QUANTITY)
