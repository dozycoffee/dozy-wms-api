package com.dozycoffee.wms.stock_audit.adapter.`in`.web.request

import jakarta.validation.constraints.NotBlank

data class AssignStockAuditRequest(
    @field:NotBlank val assignee: String?
)
