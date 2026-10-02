package com.dozycoffee.wms.global.security

import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.context.annotation.Profile
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component
import java.util.UUID

/** 로컬 개발 전용. 요청이면 고정 개발 사용자, 요청 밖 작업이면 [SystemActor]다 */
@Component
@Profile("local")
class LocalActorProvider : CurrentActorProvider {

    override suspend fun get(): Actor {
        ReactiveSecurityContextHolder.getContext().awaitSingleOrNull() ?: return SystemActor
        return UserActor(LOCAL_PRINCIPAL_ID, emptySet())
    }

    companion object {
        val LOCAL_PRINCIPAL_ID: UUID = UUID.fromString("00000000-0000-7000-8000-000000000001")
    }
}
