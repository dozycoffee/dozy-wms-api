package com.dozycoffee.wms.warehouse_member.adapter.out.event

import com.dozycoffee.wms.support.KafkaTestContainer
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.json.JsonMapper
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

/** 실제 Kafka(Testcontainers)로 토픽, 키, envelope 값, 헤더, 브로커 확인 대기와 실패 전파를 검증한다 */
class KafkaWarehouseAccessEventPublisherTest {

    private val factories: MutableList<DefaultKafkaProducerFactory<String, String>> = mutableListOf()
    private val topic: String = "test-${UUID.randomUUID().toString().take(8)}.warehouse-access"
    private val principalId: UUID = UUID.fromString("0199a3c4-7b2e-7c1a-9f3d-2b6e8a1c4d5f")
    private val eventId: String = "0199a3c2-8e5a-7f30-8c4b-9d1e2f6a0b73"
    private val zone: ZoneId = ZoneId.of("Asia/Seoul")
    private val jsonMapper: JsonMapper = JsonMapper.builder().disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS).build()

    @AfterEach
    fun cleanUp() {
        factories.forEach { it.destroy() }
    }

    private fun publisher(
        bootstrapServers: String = KafkaTestContainer.bootstrapServers,
        blockMs: Int = 20_000
    ): KafkaWarehouseAccessEventPublisher {
        val factory: DefaultKafkaProducerFactory<String, String> = DefaultKafkaProducerFactory(
            mapOf(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                ProducerConfig.ACKS_CONFIG to "all",
                ProducerConfig.MAX_BLOCK_MS_CONFIG to blockMs,
                ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG to blockMs,
                ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG to blockMs * 2
            )
        )
        factories += factory
        return KafkaWarehouseAccessEventPublisher(KafkaTemplate(factory), jsonMapper, Clock.system(zone), topic)
    }

    @Test
    fun `직원 키로 envelope 값을 토픽에 보내고 Kafka 헤더는 쓰지 않는다`() {
        runBlocking {
            publisher().publish(principalId, eventId, LocalDateTime.of(2026, 10, 12, 10, 0, 0), listOf(10L, 20L))
        }

        val record = KafkaTestContainer.consume(listOf(topic), 1).single()
        assertThat(record.key()).isEqualTo(principalId.toString())
        val value = jsonMapper.readTree(record.value())
        assertThat(value.get("eventId").asString()).isEqualTo(eventId)
        assertThat(value.get("eventType").asString()).isEqualTo("WAREHOUSE_ACCESS_CHANGED")
        assertThat(value.get("eventVersion").asInt()).isEqualTo(1)
        assertThat(value.get("occurredAt").asString()).isEqualTo("2026-10-12T10:00:00+09:00")
        assertThat(value.get("source").asString()).isEqualTo("wms")
        assertThat(value.get("data").get("principalId").asString()).isEqualTo(principalId.toString())
        val warehouseIds: List<Long> = value.get("data").get("warehouseIds").values().map { element -> element.asLong() }
        assertThat(warehouseIds).containsExactly(10L, 20L)
        assertThat(record.headers().toArray()).isEmpty()
        assertThat(value.propertyNames().asSequence().toList())
            .containsExactlyInAnyOrder("eventId", "eventType", "eventVersion", "occurredAt", "source", "data")
    }

    @Test
    fun `소속이 없으면 빈 배열을 보낸다`() {
        runBlocking { publisher().publish(principalId, eventId, LocalDateTime.of(2026, 10, 12, 10, 0, 0), emptyList()) }

        val value = jsonMapper.readTree(KafkaTestContainer.consume(listOf(topic), 1).single().value())

        assertThat(value.get("data").get("warehouseIds").size()).isEqualTo(0)
    }

    @Test
    fun `브로커에 연결할 수 없으면 예외를 던진다`() {
        assertThrows<Exception> {
            runBlocking {
                publisher(bootstrapServers = "127.0.0.1:1", blockMs = 1_000)
                    .publish(principalId, eventId, LocalDateTime.of(2026, 10, 12, 10, 0, 0), listOf(1L))
            }
        }
    }
}
