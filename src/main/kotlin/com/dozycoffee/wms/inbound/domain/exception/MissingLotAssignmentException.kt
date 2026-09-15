package com.dozycoffee.wms.inbound.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class MissingLotAssignmentException : ApplicationException(InboundItemErrorCode.MISSING_LOT_ASSIGNMENT)
