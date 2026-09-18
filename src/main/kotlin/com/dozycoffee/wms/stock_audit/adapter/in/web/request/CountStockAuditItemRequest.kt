package com.dozycoffee.wms.stock_audit.adapter.`in`.web.request

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero

data class CountStockAuditItemRequest(
    @field:NotNull @field:PositiveOrZero val countedQuantity: Int?
)
