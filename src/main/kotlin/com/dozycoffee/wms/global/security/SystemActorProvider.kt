package com.dozycoffee.wms.global.security

import org.springframework.stereotype.Component

@Component
class SystemActorProvider : CurrentActorProvider {
    override suspend fun get(): Actor = SystemActor
}
