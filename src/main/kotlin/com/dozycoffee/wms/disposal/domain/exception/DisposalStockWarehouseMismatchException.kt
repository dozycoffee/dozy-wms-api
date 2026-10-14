package com.dozycoffee.wms.disposal.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class DisposalStockWarehouseMismatchException : ApplicationException(DisposalItemErrorCode.STOCK_WAREHOUSE_MISMATCH)
