package com.dozycoffee.wms.global.security

interface CurrentAccessScopeProvider {
    suspend fun get(): AccessScope
}
