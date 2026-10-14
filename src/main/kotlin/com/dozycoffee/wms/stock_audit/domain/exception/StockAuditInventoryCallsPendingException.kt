package com.dozycoffee.wms.stock_audit.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class StockAuditInventoryCallsPendingException : ApplicationException(StockAuditErrorCode.INVENTORY_CALLS_PENDING)
