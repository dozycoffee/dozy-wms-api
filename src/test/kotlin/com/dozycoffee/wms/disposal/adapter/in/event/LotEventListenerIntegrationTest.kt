package com.dozycoffee.wms.disposal.adapter.`in`.event

import com.dozycoffee.wms.disposal.adapter.out.persistence.DisposalItemR2dbcRepository
import com.dozycoffee.wms.disposal.adapter.out.persistence.DisposalR2dbcRepository
import com.dozycoffee.wms.location_stock.adapter.out.persistence.LocationStockPersistenceAdapter
import com.dozycoffee.wms.location_stock.adapter.out.persistence.LocationStockR2dbcRepository
import com.dozycoffee.wms.location_stock.domain.enumeration.StockQualityStatus
import com.dozycoffee.wms.location_stock.domain.model.LocationStock
import com.dozycoffee.wms.product.adapter.out.persistence.ProductPersistenceAdapter
import com.dozycoffee.wms.product.adapter.out.persistence.ProductR2dbcRepository
import com.dozycoffee.wms.product.fixture.ProductTestBuilder.Companion.product
import com.dozycoffee.wms.support.KafkaTestContainer
import com.dozycoffee.wms.warehouse.adapter.out.persistence.LocationPersistenceAdapter
import com.dozycoffee.wms.warehouse.adapter.out.persistence.LocationR2dbcRepository
import com.dozycoffee.wms.warehouse.adapter.out.persistence.WarehousePersistenceAdapter
import com.dozycoffee.wms.warehouse.adapter.out.persistence.WarehouseR2dbcRepository
import com.dozycoffee.wms.warehouse.adapter.out.persistence.ZonePersistenceAdapter
import com.dozycoffee.wms.warehouse.adapter.out.persistence.ZoneR2dbcRepository
import com.dozycoffee.wms.warehouse.fixture.LocationTestBuilder.Companion.location
import com.dozycoffee.wms.warehouse.fixture.WarehouseTestBuilder.Companion.warehouse
import com.dozycoffee.wms.warehouse.fixture.ZoneTestBuilder.Companion.zone
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

/** 실제 Kafka와 MySQL(Testcontainers)로 Lot 경과 이벤트 수신, 폐기 예정 전환·폐기 신청 생성, 중복 전달, 무관한 이벤트 무시, 잘못된 메시지의 DLT 이동을 검증한다 */
@SpringBootTest
class LotEventListenerIntegrationTest {

    @Autowired private lateinit var warehousePersistenceAdapter: WarehousePersistenceAdapter
    @Autowired private lateinit var zonePersistenceAdapter: ZonePersistenceAdapter
    @Autowired private lateinit var locationPersistenceAdapter: LocationPersistenceAdapter
    @Autowired private lateinit var productPersistenceAdapter: ProductPersistenceAdapter
    @Autowired private lateinit var locationStockPersistenceAdapter: LocationStockPersistenceAdapter
    @Autowired private lateinit var disposalR2dbcRepository: DisposalR2dbcRepository
    @Autowired private lateinit var disposalItemR2dbcRepository: DisposalItemR2dbcRepository
    @Autowired private lateinit var locationStockR2dbcRepository: LocationStockR2dbcRepository
    @Autowired private lateinit var productR2dbcRepository: ProductR2dbcRepository
    @Autowired private lateinit var locationR2dbcRepository: LocationR2dbcRepository
    @Autowired private lateinit var zoneR2dbcRepository: ZoneR2dbcRepository
    @Autowired private lateinit var warehouseR2dbcRepository: WarehouseR2dbcRepository

    private var warehouseId: Long = 0L
    private var productId: Long = 0L

    @BeforeEach
    fun setUp() {
        runBlocking {
            warehouseId = requireNotNull(warehousePersistenceAdapter.save(warehouse().build()).warehouseId)
            val zoneId = requireNotNull(zonePersistenceAdapter.save(zone().warehouseId(warehouseId).build()).zoneId)
            val locationA = requireNotNull(locationPersistenceAdapter.save(location().zoneId(zoneId).locationCode("A-01").build()).locationId)
            val locationB = requireNotNull(locationPersistenceAdapter.save(location().zoneId(zoneId).locationCode("A-02").build()).locationId)
            productId = productPersistenceAdapter.save(product().build()).productId
            locationStockPersistenceAdapter.save(LocationStock.create(locationA, productId, "LOT-1", StockQualityStatus.NORMAL, 4))
            locationStockPersistenceAdapter.save(LocationStock.create(locationB, productId, "LOT-1", StockQualityStatus.NORMAL, 9))
        }
    }

    @AfterEach
    fun cleanUp() {
        runBlocking {
            disposalItemR2dbcRepository.deleteAll()
            disposalR2dbcRepository.deleteAll()
            locationStockR2dbcRepository.deleteAll()
            productR2dbcRepository.deleteAll()
            locationR2dbcRepository.deleteAll()
            zoneR2dbcRepository.deleteAll()
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

    private fun event(eventId: String, quantity: Int = 10, eventType: String = "LOT_EXPIRED"): String =
        """{"eventId":"$eventId","eventType":"$eventType","eventVersion":1,"occurredAt":"2026-10-14T01:00:00+09:00","source":"inventory","data":{"lotId":7,"productId":$productId,"lotNumber":"LOT-1","expirationDate":"2026-10-13","transfers":[{"warehouseId":$warehouseId,"sourceInventoryId":55,"disposalInventoryId":56,"quantity":$quantity}],"confirmedReservationIds":[]}}"""

    private fun disposals() = runBlocking { disposalR2dbcRepository.findAll().toList() }

    @Test
    fun `Lot 경과 이벤트를 받으면 정상 재고가 폐기 예정으로 옮겨지고 폐기 신청이 생긴다`() {
        val eventId = UUID.randomUUID().toString()
        send("7", event(eventId, quantity = 10))

        await().atMost(Duration.ofSeconds(30)).untilAsserted { assertThat(disposals()).hasSize(1) }

        val disposal = disposals().single()
        assertThat(disposal.sourceEventId).isEqualTo(eventId)
        assertThat(disposal.status).isEqualTo("REQUESTED")
        val items = runBlocking { disposalItemR2dbcRepository.findAllByDisposalId(requireNotNull(disposal.disposalId)).toList() }
        assertThat(items.map { it.quantity }).containsExactlyInAnyOrder(9, 1)
        assertThat(items.map { it.reason }).containsOnly("EXPIRED")
        val stocks = runBlocking { locationStockR2dbcRepository.findAll().toList() }
        assertThat(stocks.filter { it.qualityStatus == "DISPOSAL_SCHEDULED" }.sumOf { it.quantity ?: 0 }).isEqualTo(10)
        assertThat(stocks.filter { it.qualityStatus == "NORMAL" }.sumOf { it.quantity ?: 0 }).isEqualTo(3)
    }

    @Test
    fun `같은 이벤트가 중복 전달되어도 한 번만 처리하고 무관한 이벤트는 무시한다`() {
        val eventId = UUID.randomUUID().toString()
        send("7", event(eventId))
        send("7", event(eventId))
        send("7", event(UUID.randomUUID().toString(), eventType = "LOT_REGISTERED"))
        val marker = UUID.randomUUID().toString()
        send("7", event(marker, quantity = 1))
        await().atMost(Duration.ofSeconds(30)).untilAsserted { assertThat(disposals().map { it.sourceEventId }).contains(marker) }

        assertThat(disposals().filter { it.sourceEventId == eventId }).hasSize(1)
        assertThat(disposals()).hasSize(2)
    }

    @Test
    fun `형식이 잘못된 메시지는 재시도 없이 DLT로 가고 뒤의 메시지는 계속 처리한다`() {
        send("bad-1", "not json")
        send("bad-2", event(UUID.randomUUID().toString()).replace("\"lotNumber\":\"LOT-1\",", ""))
        val marker = UUID.randomUUID().toString()
        send("7", event(marker, quantity = 2))

        await().atMost(Duration.ofSeconds(30)).untilAsserted { assertThat(disposals().map { it.sourceEventId }).contains(marker) }

        val dead = KafkaTestContainer.consume(listOf("$TOPIC.dlt"), 2)
        assertThat(dead.map { it.key() }).containsExactlyInAnyOrder("bad-1", "bad-2")
    }

    companion object {
        private val TOPIC: String = "test-${UUID.randomUUID().toString().take(8)}.inventory.lot"

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.kafka.bootstrap-servers") { KafkaTestContainer.bootstrapServers }
            registry.add("wms.lot-event.listener.enabled") { "true" }
            registry.add("wms.lot-event.listener.topic") { TOPIC }
            registry.add("spring.kafka.consumer.group-id") { "test-${UUID.randomUUID()}" }
        }
    }
}
