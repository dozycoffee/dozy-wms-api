package com.dozycoffee.wms.return_request.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class ReturnLotNotRegisteredException : ApplicationException(ReturnItemErrorCode.LOT_NOT_REGISTERED)
