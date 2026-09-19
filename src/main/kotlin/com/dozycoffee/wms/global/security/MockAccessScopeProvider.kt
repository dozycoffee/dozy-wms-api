package com.dozycoffee.wms.global.security

import org.springframework.stereotype.Component

@Component
class MockAccessScopeProvider : CurrentAccessScopeProvider {
    override suspend fun get(): AccessScope = AccessScope(userId = "system", warehouseIds = emptyList())
}
