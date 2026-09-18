package com.dozycoffee.wms.stock_audit.domain.exception

import com.dozycoffee.wms.global.error.DomainException

class InvalidStockAuditStatusTransitionException : DomainException(StockAuditErrorCode.INVALID_STATUS_TRANSITION)
