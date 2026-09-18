package com.dozycoffee.wms.stock_audit.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class StockAuditItemNotFoundException : ApplicationException(StockAuditItemErrorCode.STOCK_AUDIT_ITEM_NOT_FOUND)
