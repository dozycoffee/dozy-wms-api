package com.dozycoffee.wms.return_request.domain.exception

import com.dozycoffee.wms.global.error.DomainException

class InvalidReturnRequestStatusTransitionException :
    DomainException(ReturnRequestErrorCode.INVALID_STATUS_TRANSITION)
