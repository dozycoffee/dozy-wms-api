package com.dozycoffee.wms.inventory_call.adapter.out.client

import com.dozycoffee.auth.starter.DozySystemClientReactiveAutoConfiguration
import com.dozycoffee.wms.inventory_call.application.port.out.InventoryApiClient
import com.dozycoffee.wms.inventory_call.application.port.out.InventoryApiResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration

/**
 * inventory 서비스를 `dozy-auth` system client 토큰으로 호출한다(ADR-0019). `dozy.auth.client.enabled`가 꺼져 있으면(로컬 개발)
 * 토큰 없이 호출한다. 주소는 `wms.inventory-call.base-url`이며 비어 있으면 재시도 대상 실패로 취급한다.
 * 연결 실패, 시간 초과, 5xx, 408, 429는 재시도하고 그 외 응답은 다시 보내도 소용없는 실패로 본다.
 */
@Component
class InventoryApiHttpClient(
    @Qualifier(DozySystemClientReactiveAutoConfiguration.WEB_CLIENT_BEAN_NAME) builders: ObjectProvider<WebClient.Builder>,
    @Value("\${wms.inventory-call.base-url:}") private val baseUrl: String,
    @Value("\${wms.inventory-call.timeout:PT10S}") private val timeout: Duration
) : InventoryApiClient {

    private val webClient: WebClient by lazy { builders.getIfAvailable { WebClient.builder() }.baseUrl(baseUrl).build() }

    override suspend fun post(path: String, idempotencyKey: String, body: String): InventoryApiResult {
        if (baseUrl.isBlank()) {
            return InventoryApiResult.Retryable("inventory 주소(wms.inventory-call.base-url)가 설정되지 않았다")
        }
        return try {
            webClient
                .post()
                .uri(path)
                .header(IDEMPOTENCY_KEY_HEADER, idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchangeToMono { response ->
                    response
                        .bodyToMono(String::class.java)
                        .defaultIfEmpty("")
                        .map { responseBody -> classify(response.statusCode().value(), responseBody) }
                }.timeout(timeout)
                .awaitSingle()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            InventoryApiResult.Retryable("${e.javaClass.simpleName}: ${e.message}")
        }
    }

    private fun classify(status: Int, responseBody: String): InventoryApiResult {
        val body: String? = responseBody.takeIf { it.isNotBlank() }
        return when {
            status in 200..299 -> InventoryApiResult.Success(body)
            status == 408 || status == 429 || status >= 500 -> InventoryApiResult.Retryable("HTTP $status")
            else -> InventoryApiResult.Permanent(status, body, "HTTP $status")
        }
    }

    companion object {
        const val IDEMPOTENCY_KEY_HEADER: String = "Idempotency-Key"
    }
}
