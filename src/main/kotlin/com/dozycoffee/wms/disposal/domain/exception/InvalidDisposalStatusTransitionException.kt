package com.dozycoffee.wms.disposal.domain.exception

import com.dozycoffee.wms.global.error.DomainException

class InvalidDisposalStatusTransitionException : DomainException(DisposalErrorCode.INVALID_STATUS_TRANSITION)
