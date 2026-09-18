package com.dozycoffee.wms.stock_audit.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class StockAuditItemsNotFullyCountedException : ApplicationException(StockAuditErrorCode.ITEMS_NOT_FULLY_COUNTED)
