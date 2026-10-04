package com.dozycoffee.wms.inbound.adapter.out.persistence

import com.dozycoffee.wms.global.config.R2dbcConfig
import com.dozycoffee.wms.inbound.domain.enumeration.DefectReason
import com.dozycoffee.wms.inbound.domain.enumeration.InspectionResult
import com.dozycoffee.wms.inbound.fixture.InboundItemTestBuilder.Companion.inboundItem
import com.dozycoffee.wms.inbound.fixture.InboundReceiptTestBuilder.Companion.inboundReceipt
import com.dozycoffee.wms.inbound.fixture.InboundTestBuilder.Companion.inbound
import com.dozycoffee.wms.product.adapter.out.persistence.ProductPersistenceAdapter
import com.dozycoffee.wms.product.adapter.out.persistence.ProductR2dbcRepository
import com.dozycoffee.wms.product.fixture.ProductTestBuilder.Companion.product
import com.dozycoffee.wms.support.SystemActorProvider
import com.dozycoffee.wms.warehouse.adapter.out.persistence.WarehousePersistenceAdapter
import com.dozycoffee.wms.warehouse.adapter.out.persistence.WarehouseR2dbcRepository
import com.dozycoffee.wms.warehouse.adapter.out.persistence.ZonePersistenceAdapter
import com.dozycoffee.wms.warehouse.adapter.out.persistence.ZoneR2dbcRepository
import com.dozycoffee.wms.warehouse.fixture.WarehouseTestBuilder.Companion.warehouse
import com.dozycoffee.wms.warehouse.fixture.ZoneTestBuilder.Companion.zone
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.context.annotation.Import
import java.time.LocalDate

@DataR2dbcTest
@Import(
    R2dbcConfig::class,
    SystemActorProvider::class,
    ProductPersistenceAdapter::class,
    WarehousePersistenceAdapter::class,
    ZonePersistenceAdapter::class,
    InboundPersistenceAdapter::class,
    InboundItemPersistenceAdapter::class,
    InboundReceiptPersistenceAdapter::class
)
class InboundReceiptPersistenceAdapterTest {

    @Autowired
    private lateinit var productPersistenceAdapter: ProductPersistenceAdapter

    @Autowired
    private lateinit var warehousePersistenceAdapter: WarehousePersistenceAdapter

    @Autowired
    private lateinit var zonePersistenceAdapter: ZonePersistenceAdapter

    @Autowired
    private lateinit var inboundPersistenceAdapter: InboundPersistenceAdapter

    @Autowired
    private lateinit var inboundItemPersistenceAdapter: InboundItemPersistenceAdapter

    @Autowired
    private lateinit var inboundReceiptPersistenceAdapter: InboundReceiptPersistenceAdapter

    @Autowired
    private lateinit var inboundReceiptR2dbcRepository: InboundReceiptR2dbcRepository

    @Autowired
    private lateinit var inboundItemR2dbcRepository: InboundItemR2dbcRepository

    @Autowired
    private lateinit var inboundR2dbcRepository: InboundR2dbcRepository

    @Autowired
    private lateinit var productR2dbcRepository: ProductR2dbcRepository

    @Autowired
    private lateinit var zoneR2dbcRepository: ZoneR2dbcRepository

    @Autowired
    private lateinit var warehouseR2dbcRepository: WarehouseR2dbcRepository

    @AfterEach
    fun cleanUp() {
        runTest {
            inboundReceiptR2dbcRepository.deleteAll()
            inboundItemR2dbcRepository.deleteAll()
            inboundR2dbcRepository.deleteAll()
            productR2dbcRepository.deleteAll()
            zoneR2dbcRepository.deleteAll()
            warehouseR2dbcRepository.deleteAll()
        }
    }

    private suspend fun createInboundItemIds(count: Int): List<Long> {
        val warehouseId = requireNotNull(warehousePersistenceAdapter.save(warehouse().build()).warehouseId)
        val zoneId = requireNotNull(zonePersistenceAdapter.save(zone().warehouseId(warehouseId).build()).zoneId)
        val productId = requireNotNull(productPersistenceAdapter.save(product().build()).productId)
        val inboundId = requireNotNull(inboundPersistenceAdapter.save(inbound().warehouseId(warehouseId).build()).inboundId)
        return (1..count).map {
            requireNotNull(
                inboundItemPersistenceAdapter.save(
                    inboundItem().inboundId(inboundId).productId(productId).zoneId(zoneId).build()
                ).inboundItemId
            )
        }
    }

    @Test
    fun `수령 라인을 저장하면 ID가 채번되고 정보가 정상적으로 왕복된다`() = runTest {
        val itemId = createInboundItemIds(1).first()
        val receipt = inboundReceipt()
            .inboundItemId(itemId).lotNumber("LOT-1").quantity(7)
            .manufactureDate(LocalDate.of(2026, 9, 1)).expirationDate(LocalDate.of(2027, 9, 1))
            .defective(DefectReason.QUALITY)
            .build()

        val saved = inboundReceiptPersistenceAdapter.saveAll(listOf(receipt)).first()
        val found = inboundReceiptPersistenceAdapter.findAllByInboundItemIds(listOf(itemId)).first()

        assertThat(saved.inboundReceiptId).isNotNull
        assertThat(found.lotNumber).isEqualTo("LOT-1")
        assertThat(found.quantity).isEqualTo(7)
        assertThat(found.manufactureDate).isEqualTo(LocalDate.of(2026, 9, 1))
        assertThat(found.expirationDate).isEqualTo(LocalDate.of(2027, 9, 1))
        assertThat(found.inspectionResult).isEqualTo(InspectionResult.DEFECTIVE)
        assertThat(found.defectReason).isEqualTo(DefectReason.QUALITY)
    }

    @Test
    fun `유통기한과 불량 사유가 없는 정상 라인도 왕복된다`() = runTest {
        val itemId = createInboundItemIds(1).first()

        inboundReceiptPersistenceAdapter.saveAll(listOf(inboundReceipt().inboundItemId(itemId).build()))
        val found = inboundReceiptPersistenceAdapter.findAllByInboundItemIds(listOf(itemId)).first()

        assertThat(found.expirationDate).isNull()
        assertThat(found.defectReason).isNull()
        assertThat(found.inspectionResult).isEqualTo(InspectionResult.NORMAL)
    }

    @Test
    fun `여러 입고 상품의 수령 라인을 IN 조건으로 한 번에 조회한다`() = runTest {
        val (itemA, itemB, itemC) = createInboundItemIds(3)
        inboundReceiptPersistenceAdapter.saveAll(
            listOf(
                inboundReceipt().inboundItemId(itemA).lotNumber("A-1").build(),
                inboundReceipt().inboundItemId(itemA).lotNumber("A-2").build(),
                inboundReceipt().inboundItemId(itemB).lotNumber("B-1").build(),
                inboundReceipt().inboundItemId(itemC).lotNumber("C-1").build()
            )
        )

        val result = inboundReceiptPersistenceAdapter.findAllByInboundItemIds(listOf(itemA, itemB))

        assertThat(result.map { it.lotNumber }).containsExactlyInAnyOrder("A-1", "A-2", "B-1")
    }

    @Test
    fun `조회 대상 ID가 비어 있으면 빈 목록을 반환한다`() = runTest {
        assertThat(inboundReceiptPersistenceAdapter.findAllByInboundItemIds(emptyList())).isEmpty()
    }
}
