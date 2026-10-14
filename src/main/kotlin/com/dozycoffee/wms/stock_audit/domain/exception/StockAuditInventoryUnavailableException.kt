package com.dozycoffee.wms.stock_audit.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class StockAuditInventoryUnavailableException : ApplicationException(StockAuditErrorCode.INVENTORY_UNAVAILABLE)
