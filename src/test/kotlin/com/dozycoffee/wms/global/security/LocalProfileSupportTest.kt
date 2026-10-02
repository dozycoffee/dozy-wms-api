package com.dozycoffee.wms.global.security

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.mock.env.MockEnvironment
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder

class LocalProfileSupportTest {

    @Nested
    inner class 로컬_행위자 {

        private val provider = LocalActorProvider()

        @Test
        fun `요청 컨텍스트가 있으면 고정된 개발 사용자를 반환한다`() = runTest {
            val anonymous = AnonymousAuthenticationToken("key", "anonymous", listOf(SimpleGrantedAuthority("ROLE_ANONYMOUS")))

            val actor = mono { provider.get() }
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(anonymous))
                .awaitSingle()

            assertThat(actor).isEqualTo(UserActor(LocalActorProvider.LOCAL_PRINCIPAL_ID, emptySet()))
        }

        @Test
        fun `요청 밖 작업이면 시스템 행위자를 반환한다`() = runTest {
            assertThat(provider.get()).isEqualTo(SystemActor)
        }
    }

    @Nested
    inner class 로컬_프로필_가드 {

        @Test
        fun `local 프로필만 켜져 있으면 통과한다`() {
            val environment = MockEnvironment().apply { setActiveProfiles("local") }

            assertThatCode { LocalProfileGuard(environment) }.doesNotThrowAnyException()
        }

        @Test
        fun `prod 프로필과 함께 켜지면 기동에 실패한다`() {
            val environment = MockEnvironment().apply { setActiveProfiles("local", "prod") }

            assertThatThrownBy { LocalProfileGuard(environment) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("prod")
        }
    }
}
