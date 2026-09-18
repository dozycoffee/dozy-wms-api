package com.dozycoffee.wms.stock_audit.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class StockAuditNotFoundException : ApplicationException(StockAuditErrorCode.STOCK_AUDIT_NOT_FOUND)
