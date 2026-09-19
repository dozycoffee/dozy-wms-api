package com.dozycoffee.wms.outbound.adapter.out.persistence

import com.dozycoffee.wms.global.config.R2dbcConfig
import com.dozycoffee.wms.global.security.MockAccessScopeProvider
import com.dozycoffee.wms.outbound.domain.enumeration.OutboundStatus
import com.dozycoffee.wms.outbound.domain.model.Outbound
import com.dozycoffee.wms.outbound.fixture.OutboundTestBuilder.Companion.outbound
import com.dozycoffee.wms.warehouse.adapter.out.persistence.WarehousePersistenceAdapter
import com.dozycoffee.wms.warehouse.adapter.out.persistence.WarehouseR2dbcRepository
import com.dozycoffee.wms.warehouse.fixture.WarehouseTestBuilder.Companion.warehouse
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.context.annotation.Import

@DataR2dbcTest
@Import(R2dbcConfig::class, MockAccessScopeProvider::class, WarehousePersistenceAdapter::class, OutboundPersistenceAdapter::class)
class OutboundPersistenceAdapterTest {

    @Autowired
    private lateinit var warehousePersistenceAdapter: WarehousePersistenceAdapter

    @Autowired
    private lateinit var outboundPersistenceAdapter: OutboundPersistenceAdapter

    @Autowired
    private lateinit var warehouseR2dbcRepository: WarehouseR2dbcRepository

    @Autowired
    private lateinit var outboundR2dbcRepository: OutboundR2dbcRepository

    @AfterEach
    fun cleanUp() {
        runTest { outboundR2dbcRepository.deleteAll() }
        warehouseR2dbcRepository.deleteAll().block()
    }

    private fun createWarehouseId(): Long {
        return requireNotNull(
            warehousePersistenceAdapter.save(warehouse().build()).map { requireNotNull(it.warehouseId) }.block()
        )
    }

    @Test
    fun `출고를 저장하면 ID가 채번되고 정보가 정상적으로 왕복된다`() = runTest {
        val warehouseId = createWarehouseId()
        val newOutbound: Outbound = outbound().warehouseId(warehouseId).build()

        val saved = outboundPersistenceAdapter.save(newOutbound)
        val found = outboundPersistenceAdapter.findById(requireNotNull(saved.outboundId))

        assertThat(found).isNotNull
        assertThat(found?.warehouseId).isEqualTo(warehouseId)
        assertThat(found?.status).isEqualTo(OutboundStatus.REQUESTED)
    }

    @Test
    fun `존재하지 않는 ID로 조회하면 null을 반환한다`() = runTest {
        assertThat(outboundPersistenceAdapter.findById(999_999L)).isNull()
    }

    @Test
    fun `상태로 목록을 필터링한다`() = runTest {
        val warehouseId = createWarehouseId()
        val requested: Outbound = outbound().warehouseId(warehouseId).build()
        val savedRequested = outboundPersistenceAdapter.save(requested)

        val picking: Outbound = outbound().warehouseId(warehouseId).build()
        picking.startPicking()
        outboundPersistenceAdapter.save(picking)

        val result = outboundPersistenceAdapter.findAll(OutboundStatus.REQUESTED).toList()

        assertThat(result).hasSize(1)
        assertThat(result.first().outboundId).isEqualTo(savedRequested.outboundId)
    }
}
