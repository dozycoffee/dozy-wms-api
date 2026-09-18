package com.dozycoffee.wms.inventory.application.port.`in`.result

data class LotDetailResult(
    val lot: LotResult,
    val distribution: List<LotDistributionResult>
)
