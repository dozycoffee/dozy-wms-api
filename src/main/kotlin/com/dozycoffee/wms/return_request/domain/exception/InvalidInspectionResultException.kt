package com.dozycoffee.wms.return_request.domain.exception

import com.dozycoffee.wms.global.error.DomainException

class InvalidInspectionResultException : DomainException(ReturnItemErrorCode.INVALID_INSPECTION_RESULT)
