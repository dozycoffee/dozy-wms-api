package com.dozycoffee.wms.global.security

data class AccessScope(val userId: String, val warehouseIds: List<Long> = emptyList())
