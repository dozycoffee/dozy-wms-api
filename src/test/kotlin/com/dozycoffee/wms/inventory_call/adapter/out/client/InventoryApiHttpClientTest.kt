package com.dozycoffee.wms.inventory_call.adapter.out.client

import com.dozycoffee.wms.inventory_call.application.port.out.InventoryApiResult
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.support.StaticListableBeanFactory
import org.springframework.web.reactive.function.client.WebClient
import java.net.InetSocketAddress
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference

class InventoryApiHttpClientTest {

    private lateinit var server: HttpServer
    private val lastRequest = AtomicReference<RecordedRequest>()
    private var status: Int = 200
    private var responseBody: String = ""
    private var delayMillis: Long = 0

    private data class RecordedRequest(val method: String, val path: String, val headers: Map<String, String>, val body: String)

    @BeforeEach
    fun setUp() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange -> handle(exchange) }
        server.start()
    }

    @AfterEach
    fun tearDown() {
        server.stop(0)
    }

    private fun handle(exchange: HttpExchange) {
        val body = exchange.requestBody.readAllBytes().toString(Charsets.UTF_8)
        lastRequest.set(
            RecordedRequest(
                exchange.requestMethod,
                exchange.requestURI.path,
                exchange.requestHeaders.entries.associate { it.key.lowercase() to it.value.first() },
                body
            )
        )
        if (delayMillis > 0) Thread.sleep(delayMillis)
        val bytes = responseBody.toByteArray()
        exchange.sendResponseHeaders(status, if (bytes.isEmpty()) -1 else bytes.size.toLong())
        if (bytes.isNotEmpty()) exchange.responseBody.use { it.write(bytes) }
        exchange.close()
    }

    private fun client(baseUrl: String = "http://127.0.0.1:${server.address.port}", timeout: Duration = Duration.ofSeconds(2)): InventoryApiHttpClient {
        val beanFactory = StaticListableBeanFactory()
        beanFactory.addBean("builder", WebClient.builder())
        val provider: ObjectProvider<WebClient.Builder> = beanFactory.getBeanProvider(WebClient.Builder::class.java)
        return InventoryApiHttpClient(provider, baseUrl, timeout)
    }

    @Test
    fun `멱등 키 헤더와 JSON 본문을 담아 POST하고 2xx면 성공으로 돌려준다`() = runTest {
        status = 200
        responseBody = "{\"quantityAfter\":10}"

        val result = client().post("/api/v1/inbound-receipts", "wms-inbound-receipt-1", "{\"quantity\":10}")

        assertThat(result).isEqualTo(InventoryApiResult.Success("{\"quantityAfter\":10}"))
        val request = lastRequest.get()
        assertThat(request.method).isEqualTo("POST")
        assertThat(request.path).isEqualTo("/api/v1/inbound-receipts")
        assertThat(request.headers["idempotency-key"]).isEqualTo("wms-inbound-receipt-1")
        assertThat(request.headers["content-type"]).startsWith("application/json")
        assertThat(request.body).isEqualTo("{\"quantity\":10}")
    }

    @Test
    fun `본문이 없는 2xx도 성공이고 본문은 null이다`() = runTest {
        status = 204

        val result = client().post("/api/v1/x", "k", "{}")

        assertThat(result).isEqualTo(InventoryApiResult.Success(null))
    }

    @Test
    fun `5xx, 408, 429는 재시도 대상이다`() = runTest {
        for (code in listOf(500, 503, 408, 429)) {
            status = code

            val result = client().post("/api/v1/x", "k", "{}")

            assertThat(result).describedAs("HTTP $code").isEqualTo(InventoryApiResult.Retryable("HTTP $code"))
        }
    }

    @Test
    fun `그 외 4xx는 다시 보내도 소용없는 실패이고 응답 본문을 남긴다`() = runTest {
        for (code in listOf(400, 404, 409, 422)) {
            status = code
            responseBody = "{\"code\":\"INV_X\"}"

            val result = client().post("/api/v1/x", "k", "{}")

            assertThat(result).describedAs("HTTP $code")
                .isEqualTo(InventoryApiResult.Permanent(code, "{\"code\":\"INV_X\"}", "HTTP $code"))
        }
    }

    @Test
    fun `시간 초과는 재시도 대상이다`() = runTest {
        delayMillis = 1_000

        val result = client(timeout = Duration.ofMillis(100)).post("/api/v1/x", "k", "{}")

        assertThat(result).isInstanceOf(InventoryApiResult.Retryable::class.java)
    }

    @Test
    fun `연결할 수 없으면 재시도 대상이다`() = runTest {
        val closedPort = server.address.port.also { server.stop(0) }

        val result = client(baseUrl = "http://127.0.0.1:$closedPort").post("/api/v1/x", "k", "{}")

        assertThat(result).isInstanceOf(InventoryApiResult.Retryable::class.java)
    }

    @Test
    fun `주소가 설정되지 않았으면 재시도 대상이다`() = runTest {
        val result = client(baseUrl = "").post("/api/v1/x", "k", "{}")

        assertThat(result).isInstanceOf(InventoryApiResult.Retryable::class.java)
        assertThat((result as InventoryApiResult.Retryable).reason).contains("wms.inventory-call.base-url")
    }
}
