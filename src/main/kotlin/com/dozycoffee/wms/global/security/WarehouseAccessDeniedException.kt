package com.dozycoffee.wms.global.security

import com.dozycoffee.wms.global.error.ApplicationException
import com.dozycoffee.wms.global.error.CommonErrorCode

class WarehouseAccessDeniedException : ApplicationException(CommonErrorCode.WAREHOUSE_ACCESS_DENIED)
