package com.dozycoffee.wms.global.config

import com.dozycoffee.wms.global.security.CurrentAccessScopeProvider
import kotlinx.coroutines.reactor.mono
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.ReactiveAuditorAware
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing

@Configuration
@EnableR2dbcAuditing
class R2dbcConfig(
    private val currentAccessScopeProvider: CurrentAccessScopeProvider
) {

    @Bean
    fun auditorAware(): ReactiveAuditorAware<String> =
        ReactiveAuditorAware { mono { currentAccessScopeProvider.get().userId } }
}
