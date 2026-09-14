package com.dozycoffee.wms.inventory.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class AllocationNotFoundException : ApplicationException(AllocationErrorCode.ALLOCATION_NOT_FOUND)
