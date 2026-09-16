package com.dozycoffee.wms.inventory.adapter.out.persistence

import com.dozycoffee.wms.global.config.R2dbcConfig
import com.dozycoffee.wms.inventory.domain.enumeration.LotStatus
import com.dozycoffee.wms.inventory.fixture.LotTestBuilder.Companion.lot
import com.dozycoffee.wms.product.adapter.out.persistence.ProductPersistenceAdapter
import com.dozycoffee.wms.product.adapter.out.persistence.ProductR2dbcRepository
import com.dozycoffee.wms.product.fixture.ProductTestBuilder.Companion.product
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.context.annotation.Import
import java.time.LocalDate

@DataR2dbcTest
@Import(R2dbcConfig::class, ProductPersistenceAdapter::class, LotPersistenceAdapter::class)
class LotPersistenceAdapterTest {

    @Autowired
    private lateinit var productPersistenceAdapter: ProductPersistenceAdapter

    @Autowired
    private lateinit var lotPersistenceAdapter: LotPersistenceAdapter

    @Autowired
    private lateinit var productR2dbcRepository: ProductR2dbcRepository

    @Autowired
    private lateinit var lotR2dbcRepository: LotR2dbcRepository

    @AfterEach
    fun cleanUp() = runTest {
        lotR2dbcRepository.deleteAll()
        productR2dbcRepository.deleteAll()
    }

    @Test
    fun `Lot을 저장하면 ID가 채번되고 정보가 정상적으로 왕복된다`() = runTest {
        val productId = requireNotNull(productPersistenceAdapter.save(product().build()).productId)
        val newLot = lot().productId(productId).build()

        val saved = lotPersistenceAdapter.save(newLot)
        val found = lotPersistenceAdapter.findById(requireNotNull(saved.lotId))

        assertThat(found).isNotNull
        assertThat(found?.lotNumber).isEqualTo(newLot.lotNumber)
        assertThat(found?.productId).isEqualTo(productId)
        assertThat(found?.expirationDate).isEqualTo(LocalDate.of(2026, 12, 31))
        assertThat(found?.lotStatus).isEqualTo(LotStatus.NORMAL)
    }

    @Test
    fun `존재하지 않는 ID로 조회하면 null을 반환한다`() = runTest {
        val found = lotPersistenceAdapter.findById(999_999L)

        assertThat(found).isNull()
    }

    @Test
    fun `같은 상품에 이미 등록된 Lot 번호는 존재하는 것으로 판단한다`() = runTest {
        val productId = requireNotNull(productPersistenceAdapter.save(product().build()).productId)
        lotPersistenceAdapter.save(lot().productId(productId).lotNumber("LOT-EXISTS").build())

        assertThat(lotPersistenceAdapter.existsByProductIdAndLotNumber(productId, "LOT-EXISTS")).isTrue()
        assertThat(lotPersistenceAdapter.existsByProductIdAndLotNumber(productId, "LOT-NOT-EXISTS")).isFalse()
    }

    @Test
    fun `상품 ID로 Lot 목록을 조회한다`() = runTest {
        val productId = requireNotNull(productPersistenceAdapter.save(product().build()).productId)
        val otherProductId = requireNotNull(
            productPersistenceAdapter.save(product().productCode("PRD-OTHER").build()).productId
        )
        lotPersistenceAdapter.save(lot().productId(productId).lotNumber("LOT-001").build())
        lotPersistenceAdapter.save(lot().productId(productId).lotNumber("LOT-002").build())
        lotPersistenceAdapter.save(lot().productId(otherProductId).lotNumber("LOT-003").build())

        val result = lotPersistenceAdapter.findAllByProductId(productId).toList()

        assertThat(result).hasSize(2)
    }

    @Test
    fun `EXPIRED가 아니면서 기준일 이내로 유통기한이 다가온 Lot만 조회한다`() = runTest {
        val productId = requireNotNull(productPersistenceAdapter.save(product().build()).productId)
        val today = LocalDate.now()
        val withinThreshold = lotPersistenceAdapter.save(
            lot().productId(productId).lotNumber("LOT-WITHIN").expirationDate(today.plusDays(10)).build()
        )
        lotPersistenceAdapter.save(
            lot().productId(productId).lotNumber("LOT-FAR").expirationDate(today.plusDays(60)).build()
        )
        val alreadyExpired = lot().productId(productId).lotNumber("LOT-EXPIRED").expirationDate(today.minusDays(1))
            .build()
        alreadyExpired.markExpired()
        lotPersistenceAdapter.save(alreadyExpired)

        val result = lotPersistenceAdapter
            .findAllByLotStatusNotAndExpirationDateLessThanEqual(LotStatus.EXPIRED, today.plusDays(30))
            .toList()

        assertThat(result).hasSize(1)
        assertThat(result.first().lotId).isEqualTo(withinThreshold.lotId)
    }
}
