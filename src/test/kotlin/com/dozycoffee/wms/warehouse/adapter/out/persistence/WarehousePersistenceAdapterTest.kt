package com.dozycoffee.wms.warehouse.adapter.out.persistence

import com.dozycoffee.wms.global.config.R2dbcConfig
import com.dozycoffee.wms.support.SystemActorProvider
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.model.Warehouse
import com.dozycoffee.wms.warehouse.fixture.WarehouseTestBuilder.Companion.warehouse
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.context.annotation.Import
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.await

@DataR2dbcTest
@Import(R2dbcConfig::class, SystemActorProvider::class, WarehousePersistenceAdapter::class)
class WarehousePersistenceAdapterTest {

    @Autowired
    private lateinit var warehousePersistenceAdapter: WarehousePersistenceAdapter

    @Autowired
    private lateinit var warehouseR2dbcRepository: WarehouseR2dbcRepository

    @Autowired
    private lateinit var databaseClient: DatabaseClient

    @AfterEach
    fun cleanUp() = runTest {
        warehouseR2dbcRepository.deleteAll()
    }

    @Test
    fun `창고를 저장하면 ID가 채번되고 상태가 정상적으로 왕복된다`() = runTest {
        val warehouse: Warehouse = warehouse().warehouseStatus(AvailabilityStatus.AVAILABLE).build()

        val saved = warehousePersistenceAdapter.save(warehouse)
        val found = requireNotNull(warehousePersistenceAdapter.findById(requireNotNull(saved.warehouseId)))

        assertThat(found.warehouseId).isNotNull()
        assertThat(found.warehouseName).isEqualTo(warehouse.warehouseName)
        assertThat(found.address).isEqualTo(warehouse.address)
        assertThat(found.warehouseStatus).isEqualTo(AvailabilityStatus.AVAILABLE)
    }

    @Test
    fun `존재하지 않는 ID로 조회하면 빈 결과를 반환한다`() = runTest {
        assertThat(warehousePersistenceAdapter.findById(999_999L)).isNull()
    }

    @Test
    fun `이미 저장된 창고를 다시 저장해도 생성 시각이 유지된다`() = runTest {
        val saved: Warehouse = warehousePersistenceAdapter.save(warehouse().build())
        val warehouseId: Long = requireNotNull(saved.warehouseId)
        val beforeUpdate: WarehouseEntity = requireNotNull(warehouseR2dbcRepository.findById(warehouseId))
        saved.deactivate()

        warehousePersistenceAdapter.save(saved)
        val afterUpdate = requireNotNull(warehouseR2dbcRepository.findById(warehouseId))

        assertThat(afterUpdate.warehouseStatus).isEqualTo(AvailabilityStatus.UNAVAILABLE.name)
        assertThat(afterUpdate.createdAt).isEqualTo(beforeUpdate.createdAt)
        assertThat(afterUpdate.createdBy).isEqualTo(beforeUpdate.createdBy)
    }

    private suspend fun saveWarehouse(name: String): Long =
        requireNotNull(warehousePersistenceAdapter.save(warehouse().warehouseName(name).build()).warehouseId)

    @Test
    fun `목록 조회는 창고 ID 오름차순으로 모든 창고를 반환한다`() = runTest {
        val first: Long = saveWarehouse("서울 본사 창고")
        val second: Long = saveWarehouse("경기 물류센터")

        val result = warehousePersistenceAdapter.findAll(null).toList()

        assertThat(result.map { it.warehouseId }).containsExactly(first, second)
        assertThat(result.map { it.warehouseName }).containsExactly("서울 본사 창고", "경기 물류센터")
    }

    @Test
    fun `창고 ID를 주면 해당 창고만 반환한다`() = runTest {
        saveWarehouse("서울 본사 창고")
        val second: Long = saveWarehouse("경기 물류센터")
        saveWarehouse("부산 센터")

        val result = warehousePersistenceAdapter.findAll(listOf(second)).toList()

        assertThat(result.map { it.warehouseId }).containsExactly(second)
    }

    @Test
    fun `삭제 처리된 창고는 목록에서 제외한다`() = runTest {
        val kept: Long = saveWarehouse("서울 본사 창고")
        val deleted: Long = saveWarehouse("폐쇄된 창고")
        databaseClient.sql("UPDATE warehouse SET deleted_at = NOW() WHERE warehouse_id = :id").bind("id", deleted).await()

        assertThat(warehousePersistenceAdapter.findAll(null).toList().map { it.warehouseId }).containsExactly(kept)
        assertThat(warehousePersistenceAdapter.findAll(listOf(kept, deleted)).toList().map { it.warehouseId }).containsExactly(kept)
    }

    @Test
    fun `창고가 없으면 빈 목록을 반환한다`() = runTest {
        assertThat(warehousePersistenceAdapter.findAll(null).toList()).isEmpty()
    }
}
