package com.dozycoffee.wms.global.security

import com.dozycoffee.auth.core.AuthenticatedPrincipal
import com.dozycoffee.auth.core.PrincipalKey
import com.dozycoffee.auth.core.PrincipalType
import com.dozycoffee.auth.core.Realm
import com.dozycoffee.auth.starter.DozyAuthenticationToken
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import java.util.UUID

class SecurityContextActorProviderTest {

    private val provider = SecurityContextActorProvider()

    private fun dozyToken(principalId: UUID, roles: Set<String>): DozyAuthenticationToken {
        val jwt = Jwt.withTokenValue("token").header("alg", "RS256").claim("sub", "employee:$principalId").build()
        val principal = AuthenticatedPrincipal(PrincipalKey(PrincipalType.EMPLOYEE, principalId), Realm.INTERNAL, roles, "sid")
        return DozyAuthenticationToken(jwt, principal, emptyList())
    }

    @Test
    fun `Auth 토큰으로 인증된 요청이면 principalId와 role을 가진 사용자 행위자를 반환한다`() = runTest {
        val principalId = UUID.fromString("0199a3c4-7b2e-7c1a-9f3d-2b6e8a1c4d5f")

        val actor = mono { provider.get() }
            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(dozyToken(principalId, setOf("inbound_manager"))))
            .awaitSingle()

        assertThat(actor).isEqualTo(UserActor(principalId, setOf("inbound_manager")))
        assertThat(actor.auditName).isEqualTo("0199a3c4-7b2e-7c1a-9f3d-2b6e8a1c4d5f")
    }

    @Test
    fun `보안 컨텍스트가 없으면 요청 밖 작업이므로 시스템 행위자를 반환한다`() = runTest {
        assertThat(provider.get()).isEqualTo(SystemActor)
    }

    @Test
    fun `Auth 토큰이 아닌 인증이면 행위자를 특정할 수 없어 실패한다`() = runTest {
        val anonymous = AnonymousAuthenticationToken("key", "anonymous", listOf(SimpleGrantedAuthority("ROLE_ANONYMOUS")))

        assertThatThrownBy {
            mono { provider.get() }
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(anonymous))
                .block()
        }.isInstanceOf(IllegalStateException::class.java)
    }
}
