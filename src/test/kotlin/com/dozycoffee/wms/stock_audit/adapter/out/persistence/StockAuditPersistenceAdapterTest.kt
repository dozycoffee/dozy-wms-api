package com.dozycoffee.wms.stock_audit.adapter.out.persistence

import com.dozycoffee.wms.global.config.R2dbcConfig
import com.dozycoffee.wms.stock_audit.domain.enumeration.StockAuditStatus
import com.dozycoffee.wms.stock_audit.domain.model.StockAudit
import com.dozycoffee.wms.stock_audit.fixture.StockAuditTestBuilder.Companion.stockAudit
import com.dozycoffee.wms.warehouse.adapter.out.persistence.WarehousePersistenceAdapter
import com.dozycoffee.wms.warehouse.adapter.out.persistence.WarehouseR2dbcRepository
import com.dozycoffee.wms.warehouse.adapter.out.persistence.ZonePersistenceAdapter
import com.dozycoffee.wms.warehouse.adapter.out.persistence.ZoneR2dbcRepository
import com.dozycoffee.wms.warehouse.fixture.WarehouseTestBuilder.Companion.warehouse
import com.dozycoffee.wms.warehouse.fixture.ZoneTestBuilder.Companion.zone
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.context.annotation.Import

@DataR2dbcTest
@Import(
    R2dbcConfig::class,
    WarehousePersistenceAdapter::class,
    ZonePersistenceAdapter::class,
    StockAuditPersistenceAdapter::class
)
class StockAuditPersistenceAdapterTest {

    @Autowired
    private lateinit var warehousePersistenceAdapter: WarehousePersistenceAdapter

    @Autowired
    private lateinit var zonePersistenceAdapter: ZonePersistenceAdapter

    @Autowired
    private lateinit var stockAuditPersistenceAdapter: StockAuditPersistenceAdapter

    @Autowired
    private lateinit var warehouseR2dbcRepository: WarehouseR2dbcRepository

    @Autowired
    private lateinit var zoneR2dbcRepository: ZoneR2dbcRepository

    @Autowired
    private lateinit var stockAuditR2dbcRepository: StockAuditR2dbcRepository

    @AfterEach
    fun cleanUp() {
        runTest { stockAuditR2dbcRepository.deleteAll() }
        zoneR2dbcRepository.deleteAll().block()
        warehouseR2dbcRepository.deleteAll().block()
    }

    private fun createWarehouseAndZone(): Pair<Long, Long> {
        val warehouseId: Long = requireNotNull(
            warehousePersistenceAdapter.save(warehouse().build()).map { requireNotNull(it.warehouseId) }.block()
        )
        val zoneId: Long = requireNotNull(
            zonePersistenceAdapter.save(zone().warehouseId(warehouseId).build())
                .map { requireNotNull(it.zoneId) }.block()
        )
        return warehouseId to zoneId
    }

    @Test
    fun `실사를 저장하면 ID가 채번되고 정보가 정상적으로 왕복된다`() = runTest {
        val (warehouseId, zoneId) = createWarehouseAndZone()
        val newStockAudit: StockAudit = stockAudit().warehouseId(warehouseId).zoneId(zoneId).build()

        val saved = stockAuditPersistenceAdapter.save(newStockAudit)
        val found = stockAuditPersistenceAdapter.findById(requireNotNull(saved.stockAuditId))

        assertThat(found).isNotNull
        assertThat(found?.warehouseId).isEqualTo(warehouseId)
        assertThat(found?.zoneId).isEqualTo(zoneId)
        assertThat(found?.status).isEqualTo(StockAuditStatus.SCHEDULED)
        assertThat(found?.assignee).isNull()
    }

    @Test
    fun `존재하지 않는 ID로 조회하면 null을 반환한다`() = runTest {
        assertThat(stockAuditPersistenceAdapter.findById(999_999L)).isNull()
    }

    @Test
    fun `담당자 배정 후 재저장하면 상태와 담당자가 갱신된다`() = runTest {
        val (warehouseId, zoneId) = createWarehouseAndZone()
        val saved = stockAuditPersistenceAdapter.save(stockAudit().warehouseId(warehouseId).zoneId(zoneId).build())

        saved.assign("담당자A")
        stockAuditPersistenceAdapter.save(saved)
        val found = stockAuditPersistenceAdapter.findById(requireNotNull(saved.stockAuditId))

        assertThat(found?.status).isEqualTo(StockAuditStatus.IN_PROGRESS)
        assertThat(found?.assignee).isEqualTo("담당자A")
    }

    @Test
    fun `창고와 상태로 목록을 필터링한다`() = runTest {
        val (warehouseId, zoneId) = createWarehouseAndZone()
        val scheduled = stockAuditPersistenceAdapter.save(stockAudit().warehouseId(warehouseId).zoneId(zoneId).build())
        val inProgress = stockAuditPersistenceAdapter.save(stockAudit().warehouseId(warehouseId).zoneId(zoneId).build())
        inProgress.assign("담당자A")
        stockAuditPersistenceAdapter.save(inProgress)

        val result = stockAuditPersistenceAdapter.findAll(warehouseId, StockAuditStatus.SCHEDULED).toList()

        assertThat(result).hasSize(1)
        assertThat(result.first().stockAuditId).isEqualTo(scheduled.stockAuditId)
    }
}
