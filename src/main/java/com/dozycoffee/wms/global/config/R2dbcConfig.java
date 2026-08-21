package com.dozycoffee.wms.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import reactor.core.publisher.Mono;

@Configuration
@EnableR2dbcAuditing
public class R2dbcConfig {

    // 인증 컨텍스트 도입 시 SecurityContext에서 현재 사용자로 대체
    @Bean
    public ReactiveAuditorAware<String> auditorAware() {
        return () -> Mono.just("system");
    }
}
