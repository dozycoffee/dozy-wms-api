package com.dozycoffee.wms.inventory.application.port.`in`.result

data class ExpirationScanResult(
    val expiringSoonLotCount: Int,
    val expiredLotCount: Int,
    val disposalScheduledInventoryCount: Int,
    val skippedActiveAllocationInventoryCount: Int
)
