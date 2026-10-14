package com.dozycoffee.wms.outbound.adapter.`in`.event

import com.dozycoffee.wms.outbound.adapter.out.persistence.OutboundAllocationR2dbcRepository
import com.dozycoffee.wms.outbound.adapter.out.persistence.OutboundItemR2dbcRepository
import com.dozycoffee.wms.outbound.adapter.out.persistence.OutboundPersistenceAdapter
import com.dozycoffee.wms.outbound.adapter.out.persistence.OutboundR2dbcRepository
import com.dozycoffee.wms.product.adapter.out.persistence.ProductPersistenceAdapter
import com.dozycoffee.wms.product.adapter.out.persistence.ProductR2dbcRepository
import com.dozycoffee.wms.product.fixture.ProductTestBuilder.Companion.product
import com.dozycoffee.wms.support.KafkaTestContainer
import com.dozycoffee.wms.warehouse.adapter.out.persistence.WarehousePersistenceAdapter
import com.dozycoffee.wms.warehouse.adapter.out.persistence.WarehouseR2dbcRepository
import com.dozycoffee.wms.warehouse.fixture.WarehouseTestBuilder.Companion.warehouse
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.Duration
import java.util.Properties
import java.util.UUID

/** 실제 Kafka와 MySQL(Testcontainers)로 예약 확정 이벤트 수신, 출고·할당 생성, 중복 전달, 무관한 이벤트 무시, 잘못된 메시지의 DLT 이동을 검증한다 */
@SpringBootTest
class ReservationEventListenerIntegrationTest {

    @Autowired
    private lateinit var warehousePersistenceAdapter: WarehousePersistenceAdapter

    @Autowired
    private lateinit var productPersistenceAdapter: ProductPersistenceAdapter

    @Autowired
    private lateinit var outboundPersistenceAdapter: OutboundPersistenceAdapter

    @Autowired
    private lateinit var outboundR2dbcRepository: OutboundR2dbcRepository

    @Autowired
    private lateinit var outboundItemR2dbcRepository: OutboundItemR2dbcRepository

    @Autowired
    private lateinit var outboundAllocationR2dbcRepository: OutboundAllocationR2dbcRepository

    @Autowired
    private lateinit var productR2dbcRepository: ProductR2dbcRepository

    @Autowired
    private lateinit var warehouseR2dbcRepository: WarehouseR2dbcRepository

    private var warehouseId: Long = 0L
    private var productId: Long = 0L

    @BeforeEach
    fun setUp() {
        runBlocking {
            warehouseId = requireNotNull(warehousePersistenceAdapter.save(warehouse().build()).warehouseId)
            productId = productPersistenceAdapter.save(product().build()).productId
        }
    }

    @AfterEach
    fun cleanUp() {
        runBlocking {
            outboundAllocationR2dbcRepository.deleteAll()
            outboundItemR2dbcRepository.deleteAll()
            outboundR2dbcRepository.deleteAll()
            productR2dbcRepository.deleteAll()
            warehouseR2dbcRepository.deleteAll()
        }
    }

    private fun send(key: String, value: String) {
        val properties = Properties().apply {
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaTestContainer.bootstrapServers)
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
        }
        KafkaProducer<String, String>(properties).use { it.send(ProducerRecord(TOPIC, key, value)).get() }
    }

    private fun event(reservationId: Long, eventType: String = "RESERVATION_CONFIRMED"): String =
        """{"eventId":"${UUID.randomUUID()}","eventType":"$eventType","eventVersion":1,"occurredAt":"2026-10-14T10:00:00+09:00","source":"inventory","data":{"reservationId":$reservationId,"warehouseId":$warehouseId,"channel":"OMS","externalOrderId":"ORDER-$reservationId","items":[{"productId":$productId,"quantity":6,"allocations":[{"inventoryId":55,"quantity":4,"lotId":7,"lotNumber":"A","expirationDate":"2027-01-01"},{"inventoryId":56,"quantity":2,"lotId":8,"lotNumber":"B"}]}]}}"""

    private fun outboundOf(reservationId: Long) =
        runBlocking { outboundPersistenceAdapter.findByWarehouseIdAndReservationId(warehouseId, reservationId) }

    @Test
    fun `예약 확정 이벤트를 받으면 출고와 상품, Lot 할당이 생긴다`() {
        send("$warehouseId:5201", event(5201))

        await().atMost(Duration.ofSeconds(30)).untilAsserted { assertThat(outboundOf(5201)).isNotNull }

        val outbound = requireNotNull(outboundOf(5201))
        assertThat(outbound.channel).isEqualTo("OMS")
        assertThat(outbound.externalOrderId).isEqualTo("ORDER-5201")
        assertThat(runBlocking { outboundR2dbcRepository.findById(requireNotNull(outbound.outboundId)) }?.createdBy).isEqualTo("system")
        val items = runBlocking { outboundItemR2dbcRepository.findAllByOutboundId(requireNotNull(outbound.outboundId)).toList() }
        assertThat(items.map { it.requestedQuantity }).containsExactly(6)
        val allocations = runBlocking { outboundAllocationR2dbcRepository.findAll().toList() }
        assertThat(allocations.map { it.lotNumber }).containsExactlyInAnyOrder("A", "B")
    }

    @Test
    fun `같은 이벤트가 중복 전달되어도 출고는 하나만 만들고 무관한 이벤트는 무시한다`() {
        val duplicated = event(5202)
        send("$warehouseId:5202", duplicated)
        send("$warehouseId:5202", duplicated)
        send("$warehouseId:5202", event(5202, eventType = "RESERVATION_RELEASED"))
        send("$warehouseId:5203", event(5203))
        await().atMost(Duration.ofSeconds(30)).untilAsserted { assertThat(outboundOf(5203)).isNotNull }

        assertThat(outboundOf(5202)).isNotNull
        assertThat(runBlocking { outboundR2dbcRepository.count() }).isEqualTo(2)
    }

    @Test
    fun `형식이 잘못된 메시지는 재시도 없이 DLT로 가고 뒤의 메시지는 계속 처리한다`() {
        send("bad-1", "not json")
        send("bad-2", event(5204).replace("\"reservationId\":5204,", ""))
        send("$warehouseId:5205", event(5205))

        await().atMost(Duration.ofSeconds(30)).untilAsserted { assertThat(outboundOf(5205)).isNotNull }

        val dead = KafkaTestContainer.consume(listOf("$TOPIC.dlt"), 2)
        assertThat(dead.map { it.key() }).containsExactlyInAnyOrder("bad-1", "bad-2")
        assertThat(outboundOf(5204)).isNull()
    }

    companion object {
        private val TOPIC: String = "test-${UUID.randomUUID().toString().take(8)}.inventory.reservation"

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.kafka.bootstrap-servers") { KafkaTestContainer.bootstrapServers }
            registry.add("wms.reservation-event.listener.enabled") { "true" }
            registry.add("wms.reservation-event.listener.topic") { TOPIC }
            registry.add("spring.kafka.consumer.group-id") { "test-${UUID.randomUUID()}" }
        }
    }
}
