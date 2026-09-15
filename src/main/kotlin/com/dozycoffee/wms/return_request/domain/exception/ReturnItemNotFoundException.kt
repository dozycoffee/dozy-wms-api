package com.dozycoffee.wms.return_request.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class ReturnItemNotFoundException : ApplicationException(ReturnItemErrorCode.RETURN_ITEM_NOT_FOUND)
