package com.dozycoffee.wms.return_request.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class MissingLotAssignmentException : ApplicationException(ReturnItemErrorCode.MISSING_LOT_ASSIGNMENT)
