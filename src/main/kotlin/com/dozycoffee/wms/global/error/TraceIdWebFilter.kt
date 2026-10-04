package com.dozycoffee.wms.global.error

import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * 모든 응답에 `X-Trace-Id`를 싣고 에러 응답의 `traceId`와 같은 값을 쓰게 한다 (dozy-auth api/conventions.md §4).
 *
 * Security 필터 체인보다 먼저 실행되어야 한다 — 스타터의 401/403 응답이 요청의 `X-Trace-Id` 헤더를 그대로 쓰므로,
 * 헤더가 없거나 형식이 맞지 않으면 여기서 만든 값으로 요청 헤더를 덮어써 두 경로의 traceId를 일치시킨다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class TraceIdWebFilter : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val traceId: String = exchange.request.headers.getFirst(HEADER)
            ?.takeIf { ACCEPTED_PATTERN.matches(it) }
            ?: UUID.randomUUID().toString().replace("-", "")

        val request = exchange.request.mutate().headers { it.set(HEADER, traceId) }.build()
        val tracedExchange: ServerWebExchange = exchange.mutate().request(request).build()
        tracedExchange.attributes[ATTRIBUTE] = traceId
        tracedExchange.response.headers.set(HEADER, traceId)
        return chain.filter(tracedExchange)
    }

    companion object {
        const val HEADER: String = "X-Trace-Id"
        private const val ATTRIBUTE: String = "com.dozycoffee.wms.traceId"

        /** 헤더 주입을 막기 위해 받아 쓰는 값의 형식을 제한한다 (dozy-auth 스타터와 같은 규칙) */
        private val ACCEPTED_PATTERN: Regex = Regex("[A-Za-z0-9-]{1,64}")

        fun of(exchange: ServerWebExchange): String? = exchange.getAttribute<String>(ATTRIBUTE)
    }
}
