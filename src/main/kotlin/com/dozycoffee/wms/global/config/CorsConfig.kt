package com.dozycoffee.wms.global.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

// 인증 필터가 핸들러 매핑보다 먼저 실행되므로 CORS 처리도 보안 필터 체인 안에서 한다.
// 토큰 없는 preflight(OPTIONS)와 401/403 응답에도 CORS 헤더가 붙어야 브라우저가 실제 상태 코드를 읽을 수 있다.
@Configuration
class CorsConfig(
    @param:Value("\${wms.cors.allowed-origins}") private val allowedOrigins: List<String>
) {
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOrigins = this@CorsConfig.allowedOrigins
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
        }
        return UrlBasedCorsConfigurationSource().apply { registerCorsConfiguration("/**", configuration) }
    }
}
