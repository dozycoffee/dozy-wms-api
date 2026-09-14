package com.dozycoffee.wms.inventory.domain.exception

import com.dozycoffee.wms.global.error.DomainException

class InvalidLotStatusTransitionException : DomainException(LotErrorCode.INVALID_STATUS_TRANSITION)
