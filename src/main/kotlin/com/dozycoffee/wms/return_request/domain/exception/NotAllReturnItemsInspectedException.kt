package com.dozycoffee.wms.return_request.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class NotAllReturnItemsInspectedException : ApplicationException(ReturnRequestErrorCode.NOT_ALL_ITEMS_INSPECTED)
