package com.dozycoffee.wms.product.adapter.`in`.event

import com.dozycoffee.wms.product.adapter.out.persistence.ProductR2dbcRepository
import com.dozycoffee.wms.support.KafkaTestContainer
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.Duration
import java.util.Properties
import java.util.UUID

/** 실제 Kafka와 MySQL(Testcontainers)로 상품 이벤트 수신, 사본 교체, 오래된 이벤트 무시, 잘못된 메시지의 DLT 이동을 검증한다 */
@SpringBootTest
class ProductEventListenerIntegrationTest {

    @Autowired
    private lateinit var productR2dbcRepository: ProductR2dbcRepository

    @AfterEach
    fun cleanUp() {
        runBlocking { productR2dbcRepository.deleteAll() }
    }

    private fun send(key: String, value: String) {
        val properties = Properties().apply {
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaTestContainer.bootstrapServers)
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
        }
        KafkaProducer<String, String>(properties).use { it.send(ProducerRecord(TOPIC, key, value)).get() }
    }

    private fun event(
        productId: Long,
        occurredAt: String,
        status: String = "ACTIVE",
        name: String = "예가체프",
        eventType: String = "PRODUCT_REGISTERED"
    ): String =
        """{"eventId":"${UUID.randomUUID()}","eventType":"$eventType","eventVersion":1,"occurredAt":"$occurredAt","source":"inventory","data":{"productId":$productId,"productCode":"BEAN-$productId","productName":"$name","category":"BEAN","unit":"KG","shelfLifeDays":180,"productStatus":"$status"}}"""

    private fun productOf(productId: Long) = runBlocking { productR2dbcRepository.findById(productId) }

    @Test
    fun `등록 이벤트를 받으면 상품 사본이 생기고 이후 상태 변경 이벤트로 교체된다`() {
        send("4201", event(4201, "2026-10-13T10:00:00+09:00"))

        await().atMost(Duration.ofSeconds(30)).untilAsserted {
            assertThat(productOf(4201)?.productStatus).isEqualTo("ACTIVE")
        }
        assertThat(productOf(4201)?.productCode).isEqualTo("BEAN-4201")
        assertThat(productOf(4201)?.createdBy).isEqualTo("system")

        send("4201", event(4201, "2026-10-13T10:05:00+09:00", status = "INACTIVE", eventType = "PRODUCT_STATUS_CHANGED"))

        await().atMost(Duration.ofSeconds(30)).untilAsserted {
            assertThat(productOf(4201)?.productStatus).isEqualTo("INACTIVE")
        }
    }

    @Test
    fun `오래된 이벤트가 늦게 도착해도 최신 사본을 덮지 않는다`() {
        send("4202", event(4202, "2026-10-13T10:05:00+09:00", status = "INACTIVE", name = "최신"))
        await().atMost(Duration.ofSeconds(30)).untilAsserted { assertThat(productOf(4202)?.productStatus).isEqualTo("INACTIVE") }

        send("4202", event(4202, "2026-10-13T10:00:00+09:00", status = "ACTIVE", name = "오래됨"))
        send("4203", event(4203, "2026-10-13T10:00:00+09:00"))
        await().atMost(Duration.ofSeconds(30)).untilAsserted { assertThat(productOf(4203)).isNotNull }

        assertThat(productOf(4202)?.productStatus).isEqualTo("INACTIVE")
        assertThat(productOf(4202)?.productName).isEqualTo("최신")
    }

    @Test
    fun `같은 이벤트가 중복 전달되어도 결과가 같다`() {
        val duplicated = event(4204, "2026-10-13T10:00:00+09:00")
        send("4204", duplicated)
        send("4204", duplicated)
        send("4205", event(4205, "2026-10-13T10:00:00+09:00"))
        await().atMost(Duration.ofSeconds(30)).untilAsserted { assertThat(productOf(4205)).isNotNull }

        assertThat(productOf(4204)?.productName).isEqualTo("예가체프")
        assertThat(runBlocking { productR2dbcRepository.count() }).isEqualTo(2)
    }

    @Test
    fun `형식이 잘못된 메시지는 재시도 없이 DLT로 가고 뒤의 메시지는 계속 처리한다`() {
        send("4206", "not json")
        send("4207", event(4207, "2026-10-13T10:00:00+09:00").replace("\"BEAN\"", "\"FROZEN\""))
        send("4208", event(4208, "2026-10-13T10:00:00+09:00"))

        await().atMost(Duration.ofSeconds(30)).untilAsserted { assertThat(productOf(4208)).isNotNull }

        val dead = KafkaTestContainer.consume(listOf("$TOPIC.dlt"), 2)
        assertThat(dead.map { it.key() }).containsExactlyInAnyOrder("4206", "4207")
        assertThat(productOf(4206)).isNull()
        assertThat(productOf(4207)).isNull()
    }

    companion object {
        private val TOPIC: String = "test-${UUID.randomUUID().toString().take(8)}.inventory.product"

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.kafka.bootstrap-servers") { KafkaTestContainer.bootstrapServers }
            registry.add("wms.product-event.listener.enabled") { "true" }
            registry.add("wms.product-event.listener.topic") { TOPIC }
            registry.add("spring.kafka.consumer.group-id") { "test-${UUID.randomUUID()}" }
        }
    }
}
