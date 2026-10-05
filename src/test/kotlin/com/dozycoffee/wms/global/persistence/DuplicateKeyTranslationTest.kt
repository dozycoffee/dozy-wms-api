package com.dozycoffee.wms.global.persistence

import com.dozycoffee.wms.global.config.R2dbcConfig
import com.dozycoffee.wms.global.error.BusinessException
import com.dozycoffee.wms.product.adapter.out.persistence.ProductPersistenceAdapter
import com.dozycoffee.wms.product.adapter.out.persistence.ProductR2dbcRepository
import com.dozycoffee.wms.product.domain.exception.DuplicateProductCodeException
import com.dozycoffee.wms.product.fixture.ProductTestBuilder.Companion.product
import com.dozycoffee.wms.support.SystemActorProvider
import com.dozycoffee.wms.warehouse.adapter.out.persistence.WarehousePersistenceAdapter
import com.dozycoffee.wms.warehouse.adapter.out.persistence.WarehouseR2dbcRepository
import com.dozycoffee.wms.warehouse.adapter.out.persistence.ZonePersistenceAdapter
import com.dozycoffee.wms.warehouse.adapter.out.persistence.ZoneR2dbcRepository
import com.dozycoffee.wms.warehouse.application.port.`in`.command.RegisterZoneCommand
import com.dozycoffee.wms.warehouse.application.service.ZoneService
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode
import com.dozycoffee.wms.warehouse.domain.exception.DuplicateZoneCodeException
import com.dozycoffee.wms.warehouse.fixture.WarehouseTestBuilder.Companion.warehouse
import com.dozycoffee.wms.warehouse.fixture.ZoneTestBuilder.Companion.zone
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException

@DataR2dbcTest
@Import(
    R2dbcConfig::class,
    SystemActorProvider::class,
    ProductPersistenceAdapter::class,
    ZonePersistenceAdapter::class,
    WarehousePersistenceAdapter::class,
    ZoneService::class
)
class DuplicateKeyTranslationTest {

    @Autowired
    private lateinit var productPersistenceAdapter: ProductPersistenceAdapter

    @Autowired
    private lateinit var productR2dbcRepository: ProductR2dbcRepository

    @Autowired
    private lateinit var zonePersistenceAdapter: ZonePersistenceAdapter

    @Autowired
    private lateinit var warehousePersistenceAdapter: WarehousePersistenceAdapter

    @Autowired
    private lateinit var warehouseR2dbcRepository: WarehouseR2dbcRepository

    @Autowired
    private lateinit var zoneR2dbcRepository: ZoneR2dbcRepository

    @Autowired
    private lateinit var zoneService: ZoneService

    @AfterEach
    fun cleanUp() = runTest {
        productR2dbcRepository.deleteAll()
        zoneR2dbcRepository.deleteAll()
        warehouseR2dbcRepository.deleteAll()
    }

    private suspend fun violationOf(block: suspend () -> Unit): DataIntegrityViolationException {
        try {
            block()
        } catch (e: DataIntegrityViolationException) {
            return e
        }
        throw AssertionError("무결성 위반이 발생해야 한다")
    }

    @Test
    fun `유니크 제약 위반은 중복 키로 판별된다`() = runTest {
        productPersistenceAdapter.save(product().productCode("PRD-DUP").build())

        val violation = violationOf { productPersistenceAdapter.save(product().productCode("PRD-DUP").build()) }

        assertThat(violation.isDuplicateKey()).isTrue()
    }

    @Test
    fun `외래키 위반은 중복 키로 판별되지 않는다`() = runTest {
        val violation = violationOf { zonePersistenceAdapter.save(zone().warehouseId(999_999L).build()) }

        assertThat(violation.isDuplicateKey()).isFalse()
    }

    @Test
    fun `유니크 위반은 지정한 도메인 예외로 바뀐다`() = runTest {
        productPersistenceAdapter.save(product().productCode("PRD-DUP").build())

        assertThatThrownBy {
            kotlinx.coroutines.runBlocking {
                translatingDuplicateKey({ DuplicateProductCodeException() }) {
                    productPersistenceAdapter.save(product().productCode("PRD-DUP").build())
                }
            }
        }.isInstanceOf(DuplicateProductCodeException::class.java)
    }

    @Test
    fun `외래키 위반은 도메인 예외로 바뀌지 않고 그대로 던져진다`() = runTest {
        assertThatThrownBy {
            kotlinx.coroutines.runBlocking {
                translatingDuplicateKey<Any>({ DuplicateProductCodeException() }) {
                    zonePersistenceAdapter.save(zone().warehouseId(999_999L).build())
                }
            }
        }.isInstanceOf(DataIntegrityViolationException::class.java)
            .isNotInstanceOf(BusinessException::class.java)
    }

    @Test
    fun `같은 창고에 같은 구역 코드를 두 번 등록하면 중복 구역 코드 예외가 된다`() = runTest {
        val warehouseId = requireNotNull(warehousePersistenceAdapter.save(warehouse().build()).warehouseId)
        zoneService.register(RegisterZoneCommand(warehouseId, ZoneCode.A))

        assertThatThrownBy {
            kotlinx.coroutines.runBlocking { zoneService.register(RegisterZoneCommand(warehouseId, ZoneCode.A)) }
        }.isInstanceOf(DuplicateZoneCodeException::class.java)
    }

    @Test
    fun `존재하지 않는 창고의 구역 등록은 중복 예외로 오인되지 않는다`() = runTest {
        assertThatThrownBy {
            kotlinx.coroutines.runBlocking { zoneService.register(RegisterZoneCommand(999_999L, ZoneCode.A)) }
        }.isInstanceOf(DataIntegrityViolationException::class.java)
    }
}
