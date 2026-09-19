package com.dozycoffee.wms.global.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.config.CorsRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer

// 모든 API가 @RestController(어노테이션 기반 핸들러)라 WebFluxConfigurer.addCorsMappings()로
// RequestMappingHandlerMapping에 직접 CORS 설정을 등록한다. 별도 CorsWebFilter 빈으로 등록하면
// RequestMappingHandlerMapping 자체의 CORS 처리와 이중으로 겹쳐 매핑이 없는 핸들러로 오인되어
// 정상 origin도 403으로 거부되는 문제가 있었다.
@Configuration
class CorsConfig(
    @param:Value("\${wms.cors.allowed-origins}") private val allowedOrigins: List<String>
) : WebFluxConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOrigins(*allowedOrigins.toTypedArray())
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*")
    }
}
