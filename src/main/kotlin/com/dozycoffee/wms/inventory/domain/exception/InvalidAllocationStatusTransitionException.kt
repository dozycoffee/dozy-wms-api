package com.dozycoffee.wms.inventory.domain.exception

import com.dozycoffee.wms.global.error.DomainException

class InvalidAllocationStatusTransitionException : DomainException(AllocationErrorCode.INVALID_STATUS_TRANSITION)
