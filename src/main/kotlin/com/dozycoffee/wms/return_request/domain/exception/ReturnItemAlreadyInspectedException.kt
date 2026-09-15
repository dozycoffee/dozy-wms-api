package com.dozycoffee.wms.return_request.domain.exception

import com.dozycoffee.wms.global.error.DomainException

class ReturnItemAlreadyInspectedException : DomainException(ReturnItemErrorCode.ALREADY_INSPECTED)
