package com.dozycoffee.wms.stock_audit.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class StockAuditZoneWarehouseMismatchException : ApplicationException(StockAuditErrorCode.ZONE_WAREHOUSE_MISMATCH)
