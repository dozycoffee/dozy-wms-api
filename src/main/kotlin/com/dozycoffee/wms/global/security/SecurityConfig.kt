package com.dozycoffee.wms.global.security

import com.dozycoffee.auth.starter.DozyAuthProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.config.web.server.invoke
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.ServerAuthenticationEntryPoint
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository
import org.springframework.web.cors.reactive.CorsConfigurationSource
import reactor.core.publisher.Mono

/**
 * 토큰 검증 부품(디코더, 권한 변환기, 401/403 핸들러)은 dozy-auth 스타터 빈을 그대로 쓰고,
 * 스타터 기본 체인에 없는 CORS만 더해 체인을 직접 정의한다.
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfig {

    @Bean
    @Profile("!local")
    fun securityWebFilterChain(
        http: ServerHttpSecurity,
        properties: DozyAuthProperties,
        jwtDecoder: ReactiveJwtDecoder,
        dozyJwtAuthenticationConverter: Converter<Jwt, Mono<AbstractAuthenticationToken>>,
        entryPoint: ServerAuthenticationEntryPoint,
        accessDeniedHandler: ServerAccessDeniedHandler,
        corsConfigurationSource: CorsConfigurationSource
    ): SecurityWebFilterChain =
        http {
            cors { configurationSource = corsConfigurationSource }
            authorizeExchange {
                properties.publicPaths.forEach { authorize(it, permitAll) }
                authorize(anyExchange, authenticated)
            }
            oauth2ResourceServer {
                jwt {
                    this.jwtDecoder = jwtDecoder
                    jwtAuthenticationConverter = dozyJwtAuthenticationConverter
                }
                authenticationEntryPoint = entryPoint
            }
            exceptionHandling {
                authenticationEntryPoint = entryPoint
                this.accessDeniedHandler = accessDeniedHandler
            }
            securityContextRepository = NoOpServerSecurityContextRepository.getInstance()
            csrf { disable() }
            httpBasic { disable() }
            formLogin { disable() }
            logout { disable() }
        }

    /** 로컬 개발 전용. Auth의 로그인·개발용 토큰 API가 준비되기 전까지 토큰 없이, 모든 role을 가진 개발 사용자로 호출할 수 있게 한다 */
    @Bean
    @Profile("local")
    fun localSecurityWebFilterChain(
        http: ServerHttpSecurity,
        corsConfigurationSource: CorsConfigurationSource
    ): SecurityWebFilterChain =
        http {
            cors { configurationSource = corsConfigurationSource }
            authorizeExchange { authorize(anyExchange, permitAll) }
            anonymous {
                principal = LocalActorProvider.LOCAL_PRINCIPAL_ID
                authorities = LocalActorProvider.LOCAL_ROLES.map { SimpleGrantedAuthority("ROLE_${it.code}") }
            }
            securityContextRepository = NoOpServerSecurityContextRepository.getInstance()
            csrf { disable() }
            httpBasic { disable() }
            formLogin { disable() }
            logout { disable() }
        }
}
