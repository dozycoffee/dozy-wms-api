package com.dozycoffee.wms.global.security

interface CurrentWarehouseAccessProvider {
    suspend fun current(): WarehouseAccess
}
