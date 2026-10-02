package com.dozycoffee.wms.global.security

interface CurrentActorProvider {
    suspend fun get(): Actor
}
