package com.dozycoffee.wms.warehouse_member.adapter.out.persistence

import com.dozycoffee.wms.global.config.R2dbcConfig
import com.dozycoffee.wms.support.SystemActorProvider
import com.dozycoffee.wms.warehouse.adapter.out.persistence.WarehousePersistenceAdapter
import com.dozycoffee.wms.warehouse.adapter.out.persistence.WarehouseR2dbcRepository
import com.dozycoffee.wms.warehouse.fixture.WarehouseTestBuilder.Companion.warehouse
import com.dozycoffee.wms.warehouse_member.domain.model.WarehouseMember
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import java.util.UUID

@DataR2dbcTest
@Import(
    R2dbcConfig::class,
    SystemActorProvider::class,
    WarehousePersistenceAdapter::class,
    WarehouseMemberPersistenceAdapter::class
)
class WarehouseMemberPersistenceAdapterTest {

    @Autowired
    private lateinit var warehousePersistenceAdapter: WarehousePersistenceAdapter

    @Autowired
    private lateinit var warehouseMemberPersistenceAdapter: WarehouseMemberPersistenceAdapter

    @Autowired
    private lateinit var warehouseR2dbcRepository: WarehouseR2dbcRepository

    @Autowired
    private lateinit var warehouseMemberR2dbcRepository: WarehouseMemberR2dbcRepository

    @AfterEach
    fun cleanUp() {
        runTest { warehouseMemberR2dbcRepository.deleteAll() }
        runTest {
            warehouseR2dbcRepository.deleteAll()
        }
    }

    private suspend fun createWarehouse(): Long = requireNotNull(warehousePersistenceAdapter.save(warehouse().build()).warehouseId)

    @Test
    fun `배정을 저장하면 ID가 채번되고 정보가 왕복된다`() = runTest {
        val warehouseId: Long = createWarehouse()
        val principalId: UUID = UUID.randomUUID()

        val saved = warehouseMemberPersistenceAdapter.save(WarehouseMember.create(warehouseId, principalId))

        assertThat(saved.warehouseMemberId).isNotNull()
        val found = warehouseMemberPersistenceAdapter.findByWarehouseIdAndPrincipalId(warehouseId, principalId)
        assertThat(found?.principalId).isEqualTo(principalId)
    }

    @Test
    fun `같은 창고에 같은 사용자를 중복 배정하면 유니크 제약으로 거부된다`() = runTest {
        val warehouseId: Long = createWarehouse()
        val principalId: UUID = UUID.randomUUID()
        warehouseMemberPersistenceAdapter.save(WarehouseMember.create(warehouseId, principalId))

        assertThatThrownBy {
            kotlinx.coroutines.runBlocking {
                warehouseMemberPersistenceAdapter.save(WarehouseMember.create(warehouseId, principalId))
            }
        }.isInstanceOf(DataIntegrityViolationException::class.java)
    }

    @Test
    fun `사용자의 접근 가능한 창고 ID 집합을 조회한다`() = runTest {
        val warehouseA: Long = createWarehouse()
        val warehouseB: Long = createWarehouse()
        val principalId: UUID = UUID.randomUUID()
        warehouseMemberPersistenceAdapter.save(WarehouseMember.create(warehouseA, principalId))
        warehouseMemberPersistenceAdapter.save(WarehouseMember.create(warehouseB, principalId))
        warehouseMemberPersistenceAdapter.save(WarehouseMember.create(warehouseA, UUID.randomUUID()))

        val ids = warehouseMemberPersistenceAdapter.findWarehouseIdsByPrincipalId(principalId)

        assertThat(ids).containsExactlyInAnyOrder(warehouseA, warehouseB)
    }

    @Test
    fun `배정이 없는 사용자의 창고 ID 집합은 비어 있다`() = runTest {
        assertThat(warehouseMemberPersistenceAdapter.findWarehouseIdsByPrincipalId(UUID.randomUUID())).isEmpty()
    }

    @Test
    fun `창고별 배정 목록을 조회하고 삭제한다`() = runTest {
        val warehouseId: Long = createWarehouse()
        val principalId: UUID = UUID.randomUUID()
        val saved = warehouseMemberPersistenceAdapter.save(WarehouseMember.create(warehouseId, principalId))

        assertThat(warehouseMemberPersistenceAdapter.findAllByWarehouseId(warehouseId).toList()).hasSize(1)

        warehouseMemberPersistenceAdapter.delete(saved)

        assertThat(warehouseMemberPersistenceAdapter.findAllByWarehouseId(warehouseId).toList()).isEmpty()
    }
}
