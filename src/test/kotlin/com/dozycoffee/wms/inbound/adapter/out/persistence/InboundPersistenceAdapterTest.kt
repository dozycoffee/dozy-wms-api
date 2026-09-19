package com.dozycoffee.wms.inbound.adapter.out.persistence

import com.dozycoffee.wms.global.config.R2dbcConfig
import com.dozycoffee.wms.global.security.MockAccessScopeProvider
import com.dozycoffee.wms.inbound.domain.enumeration.InboundStatus
import com.dozycoffee.wms.inbound.domain.model.Inbound
import com.dozycoffee.wms.inbound.fixture.InboundTestBuilder.Companion.inbound
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
@Import(R2dbcConfig::class, MockAccessScopeProvider::class, WarehousePersistenceAdapter::class, InboundPersistenceAdapter::class)
class InboundPersistenceAdapterTest {

    @Autowired
    private lateinit var warehousePersistenceAdapter: WarehousePersistenceAdapter

    @Autowired
    private lateinit var inboundPersistenceAdapter: InboundPersistenceAdapter

    @Autowired
    private lateinit var warehouseR2dbcRepository: WarehouseR2dbcRepository

    @Autowired
    private lateinit var inboundR2dbcRepository: InboundR2dbcRepository

    @AfterEach
    fun cleanUp() {
        runTest { inboundR2dbcRepository.deleteAll() }
        warehouseR2dbcRepository.deleteAll().block()
    }

    private fun createWarehouseId(): Long {
        return requireNotNull(
            warehousePersistenceAdapter.save(warehouse().build()).map { requireNotNull(it.warehouseId) }.block()
        )
    }

    @Test
    fun `입고를 저장하면 ID가 채번되고 정보가 정상적으로 왕복된다`() = runTest {
        val warehouseId = createWarehouseId()
        val newInbound: Inbound = inbound().warehouseId(warehouseId).build()
        newInbound.markWaiting()

        val saved = inboundPersistenceAdapter.save(newInbound)
        val found = inboundPersistenceAdapter.findById(requireNotNull(saved.inboundId))

        assertThat(found).isNotNull
        assertThat(found?.warehouseId).isEqualTo(warehouseId)
        assertThat(found?.status).isEqualTo(InboundStatus.WAITING)
    }

    @Test
    fun `존재하지 않는 ID로 조회하면 null을 반환한다`() = runTest {
        assertThat(inboundPersistenceAdapter.findById(999_999L)).isNull()
    }

    @Test
    fun `상태로 목록을 필터링한다`() = runTest {
        val warehouseId = createWarehouseId()
        val waitingInbound: Inbound = inbound().warehouseId(warehouseId).build()
        waitingInbound.markWaiting()
        val waiting = inboundPersistenceAdapter.save(waitingInbound)

        val processingInbound: Inbound = inbound().warehouseId(warehouseId).build()
        processingInbound.markWaiting()
        processingInbound.startProcessing()
        inboundPersistenceAdapter.save(processingInbound)

        val result = inboundPersistenceAdapter.findAll(InboundStatus.WAITING).toList()

        assertThat(result).hasSize(1)
        assertThat(result.first().inboundId).isEqualTo(waiting.inboundId)
    }
}
