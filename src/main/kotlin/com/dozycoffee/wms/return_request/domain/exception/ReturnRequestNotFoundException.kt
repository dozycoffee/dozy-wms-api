package com.dozycoffee.wms.return_request.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class ReturnRequestNotFoundException : ApplicationException(ReturnRequestErrorCode.RETURN_REQUEST_NOT_FOUND)
