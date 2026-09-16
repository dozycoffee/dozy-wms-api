package com.dozycoffee.wms.inventory.application.port.`in`

import com.dozycoffee.wms.inventory.application.port.`in`.result.ExpirationScanResult

interface ScanExpirationUseCase {
    suspend fun scan(): ExpirationScanResult
}
