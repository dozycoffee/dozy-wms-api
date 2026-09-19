package com.dozycoffee.wms.disposal.adapter.out.persistence

import com.dozycoffee.wms.disposal.domain.enumeration.DisposalStatus
import com.dozycoffee.wms.disposal.domain.model.Disposal
import com.dozycoffee.wms.disposal.fixture.DisposalTestBuilder.Companion.disposal
import com.dozycoffee.wms.global.config.R2dbcConfig
import com.dozycoffee.wms.global.security.MockAccessScopeProvider
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
@Import(R2dbcConfig::class, MockAccessScopeProvider::class, WarehousePersistenceAdapter::class, DisposalPersistenceAdapter::class)
class DisposalPersistenceAdapterTest {

    @Autowired
    private lateinit var warehousePersistenceAdapter: WarehousePersistenceAdapter

    @Autowired
    private lateinit var disposalPersistenceAdapter: DisposalPersistenceAdapter

    @Autowired
    private lateinit var warehouseR2dbcRepository: WarehouseR2dbcRepository

    @Autowired
    private lateinit var disposalR2dbcRepository: DisposalR2dbcRepository

    @AfterEach
    fun cleanUp() {
        runTest { disposalR2dbcRepository.deleteAll() }
        warehouseR2dbcRepository.deleteAll().block()
    }

    private fun createWarehouseId(): Long {
        return requireNotNull(
            warehousePersistenceAdapter.save(warehouse().build()).map { requireNotNull(it.warehouseId) }.block()
        )
    }

    @Test
    fun `폐기를 저장하면 ID가 채번되고 정보가 정상적으로 왕복된다`() = runTest {
        val warehouseId = createWarehouseId()
        val newDisposal: Disposal = disposal().warehouseId(warehouseId).build()

        val saved = disposalPersistenceAdapter.save(newDisposal)
        val found = disposalPersistenceAdapter.findById(requireNotNull(saved.disposalId))

        assertThat(found).isNotNull
        assertThat(found?.warehouseId).isEqualTo(warehouseId)
        assertThat(found?.status).isEqualTo(DisposalStatus.REQUESTED)
    }

    @Test
    fun `존재하지 않는 ID로 조회하면 null을 반환한다`() = runTest {
        assertThat(disposalPersistenceAdapter.findById(999_999L)).isNull()
    }

    @Test
    fun `상태로 목록을 필터링한다`() = runTest {
        val warehouseId = createWarehouseId()
        val requested: Disposal = disposal().warehouseId(warehouseId).build()
        val savedRequested = disposalPersistenceAdapter.save(requested)

        val approved: Disposal = disposal().warehouseId(warehouseId).build()
        approved.approve()
        disposalPersistenceAdapter.save(approved)

        val result = disposalPersistenceAdapter.findAll(DisposalStatus.REQUESTED).toList()

        assertThat(result).hasSize(1)
        assertThat(result.first().disposalId).isEqualTo(savedRequested.disposalId)
    }
}
