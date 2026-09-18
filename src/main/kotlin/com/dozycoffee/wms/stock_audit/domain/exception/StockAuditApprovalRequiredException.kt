package com.dozycoffee.wms.stock_audit.domain.exception

import com.dozycoffee.wms.global.error.DomainException

class StockAuditApprovalRequiredException : DomainException(StockAuditErrorCode.APPROVAL_REQUIRED)
