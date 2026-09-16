package com.dozycoffee.wms.return_request.adapter.out.persistence

import com.dozycoffee.wms.global.config.R2dbcConfig
import com.dozycoffee.wms.return_request.domain.enumeration.ReturnRequestStatus
import com.dozycoffee.wms.return_request.domain.model.ReturnRequest
import com.dozycoffee.wms.return_request.fixture.ReturnRequestTestBuilder.Companion.returnRequest
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
@Import(R2dbcConfig::class, WarehousePersistenceAdapter::class, ReturnRequestPersistenceAdapter::class)
class ReturnRequestPersistenceAdapterTest {

    @Autowired
    private lateinit var warehousePersistenceAdapter: WarehousePersistenceAdapter

    @Autowired
    private lateinit var returnRequestPersistenceAdapter: ReturnRequestPersistenceAdapter

    @Autowired
    private lateinit var warehouseR2dbcRepository: WarehouseR2dbcRepository

    @Autowired
    private lateinit var returnRequestR2dbcRepository: ReturnRequestR2dbcRepository

    @AfterEach
    fun cleanUp() {
        runTest { returnRequestR2dbcRepository.deleteAll() }
        warehouseR2dbcRepository.deleteAll().block()
    }

    private fun createWarehouseId(): Long {
        return requireNotNull(
            warehousePersistenceAdapter.save(warehouse().build()).map { requireNotNull(it.warehouseId) }.block()
        )
    }

    @Test
    fun `반품을 저장하면 ID가 채번되고 정보가 정상적으로 왕복된다`() = runTest {
        val warehouseId = createWarehouseId()
        val newReturnRequest: ReturnRequest = returnRequest().warehouseId(warehouseId).build()

        val saved = returnRequestPersistenceAdapter.save(newReturnRequest)
        val found = returnRequestPersistenceAdapter.findById(requireNotNull(saved.returnRequestId))

        assertThat(found).isNotNull
        assertThat(found?.warehouseId).isEqualTo(warehouseId)
        assertThat(found?.status).isEqualTo(ReturnRequestStatus.RECEIVED)
    }

    @Test
    fun `존재하지 않는 ID로 조회하면 null을 반환한다`() = runTest {
        assertThat(returnRequestPersistenceAdapter.findById(999_999L)).isNull()
    }

    @Test
    fun `상태로 목록을 필터링한다`() = runTest {
        val warehouseId = createWarehouseId()
        val received: ReturnRequest = returnRequest().warehouseId(warehouseId).build()
        val savedReceived = returnRequestPersistenceAdapter.save(received)

        val inspecting: ReturnRequest = returnRequest().warehouseId(warehouseId).build()
        inspecting.startInspecting()
        returnRequestPersistenceAdapter.save(inspecting)

        val result = returnRequestPersistenceAdapter.findAll(ReturnRequestStatus.RECEIVED).toList()

        assertThat(result).hasSize(1)
        assertThat(result.first().returnRequestId).isEqualTo(savedReceived.returnRequestId)
    }
}
