package com.dozycoffee.wms.inventory_call.adapter.out.persistence

import com.dozycoffee.wms.global.config.R2dbcConfig
import com.dozycoffee.wms.global.persistence.isDuplicateKey
import com.dozycoffee.wms.inventory_call.domain.enumeration.InventoryCallStatus
import com.dozycoffee.wms.inventory_call.domain.model.InventoryCall
import com.dozycoffee.wms.inventory_call.fixture.InventoryCallTestBuilder.Companion.NOW
import com.dozycoffee.wms.inventory_call.fixture.InventoryCallTestBuilder.Companion.inventoryCall
import com.dozycoffee.wms.support.SystemActorProvider
import com.dozycoffee.wms.warehouse.adapter.out.persistence.WarehousePersistenceAdapter
import com.dozycoffee.wms.warehouse.adapter.out.persistence.WarehouseR2dbcRepository
import com.dozycoffee.wms.warehouse.fixture.WarehouseTestBuilder.Companion.warehouse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.ReactiveTransactionManager
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait

@DataR2dbcTest
@Import(
    R2dbcConfig::class,
    SystemActorProvider::class,
    WarehousePersistenceAdapter::class,
    InventoryCallPersistenceAdapter::class
)
class InventoryCallPersistenceAdapterTest {

    @Autowired
    private lateinit var adapter: InventoryCallPersistenceAdapter

    @Autowired
    private lateinit var inventoryCallR2dbcRepository: InventoryCallR2dbcRepository

    @Autowired
    private lateinit var warehousePersistenceAdapter: WarehousePersistenceAdapter

    @Autowired
    private lateinit var warehouseR2dbcRepository: WarehouseR2dbcRepository

    @Autowired
    private lateinit var transactionManager: ReactiveTransactionManager

    @AfterEach
    fun cleanUp() {
        runTest {
            inventoryCallR2dbcRepository.deleteAll()
            warehouseR2dbcRepository.deleteAll()
        }
    }

    private suspend fun createWarehouse(): Long =
        requireNotNull(warehousePersistenceAdapter.save(warehouse().build()).warehouseId)

    private suspend fun record(warehouseId: Long, key: String, nextAttemptAt: java.time.LocalDateTime = NOW): InventoryCall =
        adapter.save(inventoryCall().warehouseId(warehouseId).idempotencyKey(key).nextAttemptAt(nextAttemptAt).build())

    @Nested
    inner class 저장과_조회 {

        @Test
        fun `저장한 호출을 멱등 키로 조회한다`() = runTest {
            val warehouseId = createWarehouse()
            val saved = record(warehouseId, "wms-inbound-receipt-1")

            val found = adapter.findByIdempotencyKey("wms-inbound-receipt-1")

            assertThat(found?.inventoryCallId).isEqualTo(saved.inventoryCallId)
            assertThat(found?.status).isEqualTo(InventoryCallStatus.PENDING)
            assertThat(found?.requestBody).isEqualTo("{\"quantity\":10}")
            assertThat(found?.nextAttemptAt).isEqualTo(NOW)
        }

        @Test
        fun `멱등 키는 대소문자를 구분한다`() = runTest {
            val warehouseId = createWarehouse()
            record(warehouseId, "wms-key-a")
            record(warehouseId, "wms-key-A")

            assertThat(adapter.findByIdempotencyKey("wms-key-A")).isNotNull
            assertThat(adapter.findByIdempotencyKey("WMS-KEY-A")).isNull()
        }

        @Test
        fun `같은 멱등 키를 두 번 저장하면 유니크 제약 위반이다`() = runTest {
            val warehouseId = createWarehouse()
            record(warehouseId, "wms-inbound-receipt-1")

            assertThatThrownBy { runBlocking { record(warehouseId, "wms-inbound-receipt-1") } }
                .isInstanceOfSatisfying(DataIntegrityViolationException::class.java) {
                    assertThat(it.isDuplicateKey()).isTrue()
                }
        }

        @Test
        fun `수정하면 상태가 갱신되고 생성 정보는 유지된다`() = runTest {
            val warehouseId = createWarehouse()
            val saved = record(warehouseId, "wms-inbound-receipt-1")
            val createdAt = inventoryCallR2dbcRepository.findById(requireNotNull(saved.inventoryCallId))?.createdAt
            saved.markSent("{\"quantityAfter\":10}", NOW.plusSeconds(1))

            adapter.save(saved)

            val reloaded = inventoryCallR2dbcRepository.findById(requireNotNull(saved.inventoryCallId))
            assertThat(reloaded?.status).isEqualTo("SENT")
            assertThat(reloaded?.responseBody).isEqualTo("{\"quantityAfter\":10}")
            assertThat(reloaded?.createdAt).isEqualTo(createdAt)
        }

        @Test
        fun `상태로 걸러 최신순으로 개수를 제한해 조회한다`() = runTest {
            val warehouseId = createWarehouse()
            val first = record(warehouseId, "k1")
            val second = record(warehouseId, "k2")
            record(warehouseId, "k3").also { it.markDead("HTTP 404", null, NOW); adapter.save(it) }

            assertThat(adapter.findAll(null, 2).toList().map { it.idempotencyKey }).containsExactly("k3", "k2")
            assertThat(adapter.findAll(InventoryCallStatus.PENDING, 10).toList().map { it.inventoryCallId })
                .containsExactly(second.inventoryCallId, first.inventoryCallId)
            assertThat(adapter.findAll(InventoryCallStatus.DEAD, 10).toList().map { it.idempotencyKey }).containsExactly("k3")
        }
    }

    @Nested
    inner class 전송_대상_선택 {

        @Test
        fun `같은 창고에서는 가장 앞선 호출만 대상이다`() = runTest {
            val warehouseId = createWarehouse()
            val first = record(warehouseId, "k1")
            record(warehouseId, "k2")

            val targets = adapter.findDispatchable(NOW, 10).toList()

            assertThat(targets.map { it.inventoryCallId }).containsExactly(first.inventoryCallId)
        }

        @Test
        fun `창고가 다르면 각 창고의 앞선 호출이 모두 대상이다`() = runTest {
            val warehouseA = createWarehouse()
            val warehouseB = requireNotNull(warehousePersistenceAdapter.save(warehouse().build()).warehouseId)
            val a1 = record(warehouseA, "a1")
            record(warehouseA, "a2")
            val b1 = record(warehouseB, "b1")

            val targets = adapter.findDispatchable(NOW, 10).toList()

            assertThat(targets.map { it.inventoryCallId }).containsExactly(a1.inventoryCallId, b1.inventoryCallId)
        }

        @Test
        fun `앞선 호출이 SENT가 되면 다음 호출이 대상이 된다`() = runTest {
            val warehouseId = createWarehouse()
            val first = record(warehouseId, "k1")
            val second = record(warehouseId, "k2")
            first.markSent(null, NOW)
            adapter.save(first)

            val targets = adapter.findDispatchable(NOW, 10).toList()

            assertThat(targets.map { it.inventoryCallId }).containsExactly(second.inventoryCallId)
        }

        @Test
        fun `앞선 호출이 DEAD면 그 창고의 뒤 호출은 대상이 아니다`() = runTest {
            val warehouseA = createWarehouse()
            val warehouseB = requireNotNull(warehousePersistenceAdapter.save(warehouse().build()).warehouseId)
            val dead = record(warehouseA, "a1")
            record(warehouseA, "a2")
            val other = record(warehouseB, "b1")
            dead.markDead("HTTP 404", null, NOW)
            adapter.save(dead)

            val targets = adapter.findDispatchable(NOW, 10).toList()

            assertThat(targets.map { it.inventoryCallId }).containsExactly(other.inventoryCallId)
        }

        @Test
        fun `DEAD를 버리면 그 창고의 큐가 다시 흐른다`() = runTest {
            val warehouseId = createWarehouse()
            val dead = record(warehouseId, "k1")
            val next = record(warehouseId, "k2")
            dead.markDead("HTTP 404", null, NOW)
            dead.discard()
            adapter.save(dead)

            val targets = adapter.findDispatchable(NOW, 10).toList()

            assertThat(targets.map { it.inventoryCallId }).containsExactly(next.inventoryCallId)
        }

        @Test
        fun `다음 시도 시각이 아직 안 된 호출은 뒤 호출도 막는다`() = runTest {
            val warehouseId = createWarehouse()
            record(warehouseId, "k1", nextAttemptAt = NOW.plusMinutes(1))
            record(warehouseId, "k2")

            assertThat(adapter.findDispatchable(NOW, 10).toList()).isEmpty()
            assertThat(adapter.findDispatchable(NOW.plusMinutes(1), 10).toList().map { it.idempotencyKey })
                .containsExactly("k1")
        }

        @Test
        fun `다른 트랜잭션이 잠근 행은 건너뛰고 그 창고의 뒤 호출도 내보내지 않는다`() = runTest {
            val warehouseA = createWarehouse()
            val warehouseB = requireNotNull(warehousePersistenceAdapter.save(warehouse().build()).warehouseId)
            val a1 = record(warehouseA, "a1")
            record(warehouseA, "a2")
            val b1 = record(warehouseB, "b1")
            val transactionalOperator = TransactionalOperator.create(transactionManager)

            val (lockedByFirst, seenBySecond) = transactionalOperator.executeAndAwait {
                val first = adapter.findDispatchable(NOW, 10).toList().map { it.inventoryCallId }
                val second = CoroutineScope(Dispatchers.Default).async {
                    transactionalOperator.executeAndAwait { adapter.findDispatchable(NOW, 10).toList().map { it.inventoryCallId } }
                }.await()
                first to second
            }

            assertThat(lockedByFirst).containsExactly(a1.inventoryCallId, b1.inventoryCallId)
            assertThat(seenBySecond).isEmpty()
        }

        @Test
        fun `개수 제한을 지킨다`() = runTest {
            val warehouses = List(3) { requireNotNull(warehousePersistenceAdapter.save(warehouse().build()).warehouseId) }
            warehouses.forEachIndexed { index, id -> record(id, "k$index") }

            assertThat(adapter.findDispatchable(NOW, 2).toList()).hasSize(2)
        }
    }
}
