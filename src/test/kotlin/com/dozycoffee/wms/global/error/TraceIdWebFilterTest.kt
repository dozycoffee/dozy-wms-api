package com.dozycoffee.wms.global.error

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

class TraceIdWebFilterTest {

    private val filter: TraceIdWebFilter = TraceIdWebFilter()

    private class CapturingChain : WebFilterChain {
        var passed: ServerWebExchange? = null

        override fun filter(exchange: ServerWebExchange): Mono<Void> {
            passed = exchange
            return Mono.empty()
        }
    }

    private fun exchangeWith(traceId: String?): MockServerWebExchange {
        val request = MockServerHttpRequest.get("/api/products")
        traceId?.let { request.header(TraceIdWebFilter.HEADER, it) }
        return MockServerWebExchange.from(request)
    }

    @Test
    fun `요청에 헤더가 없으면 새 traceId를 만들어 응답 헤더와 다음 필터의 요청 헤더에 싣는다`() {
        val exchange: MockServerWebExchange = exchangeWith(null)
        val chain = CapturingChain()

        filter.filter(exchange, chain).block()

        val traceId: String? = exchange.response.headers.getFirst(TraceIdWebFilter.HEADER)
        assertThat(traceId).matches("[0-9a-f]{32}")
        val passed: ServerWebExchange = requireNotNull(chain.passed)
        assertThat(passed.request.headers.getFirst(TraceIdWebFilter.HEADER)).isEqualTo(traceId)
        assertThat(TraceIdWebFilter.of(passed)).isEqualTo(traceId)
    }

    @Test
    fun `요청의 X-Trace-Id가 허용 형식이면 그대로 쓴다`() {
        val exchange: MockServerWebExchange = exchangeWith("4bf92f3577b34da6a3ce929d0e0e4736")
        val chain = CapturingChain()

        filter.filter(exchange, chain).block()

        assertThat(exchange.response.headers.getFirst(TraceIdWebFilter.HEADER)).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736")
        assertThat(TraceIdWebFilter.of(requireNotNull(chain.passed))).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736")
    }

    @Test
    fun `요청의 X-Trace-Id가 허용 형식이 아니면 버리고 새로 만든다`() {
        val exchange: MockServerWebExchange = exchangeWith("bad value\r\nInjected: 1")
        val chain = CapturingChain()

        filter.filter(exchange, chain).block()

        val traceId: String? = exchange.response.headers.getFirst(TraceIdWebFilter.HEADER)
        assertThat(traceId).matches("[0-9a-f]{32}")
        assertThat(requireNotNull(chain.passed).request.headers.getFirst(TraceIdWebFilter.HEADER)).isEqualTo(traceId)
    }

    @Test
    fun `65자 이상의 X-Trace-Id는 버리고 새로 만든다`() {
        val exchange: MockServerWebExchange = exchangeWith("a".repeat(65))

        filter.filter(exchange, CapturingChain()).block()

        assertThat(exchange.response.headers.getFirst(TraceIdWebFilter.HEADER)).matches("[0-9a-f]{32}")
    }
}
