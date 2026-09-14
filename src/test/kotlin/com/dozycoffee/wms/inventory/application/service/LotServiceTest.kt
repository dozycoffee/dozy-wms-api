package com.dozycoffee.wms.inventory.application.service

import com.dozycoffee.wms.inventory.application.port.`in`.command.RegisterLotCommand
import com.dozycoffee.wms.inventory.application.port.out.LotRepository
import com.dozycoffee.wms.inventory.domain.exception.DuplicateLotNumberException
import com.dozycoffee.wms.inventory.domain.exception.LotNotFoundException
import com.dozycoffee.wms.inventory.domain.model.Lot
import com.dozycoffee.wms.inventory.fixture.LotTestBuilder.Companion.lot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class LotServiceTest {

    @Mock
    private lateinit var lotRepository: LotRepository

    @InjectMocks
    private lateinit var lotService: LotService

    @Nested
    inner class Lot_등록 {

        @Test
        fun `정상적인 정보로 등록하면 저장된 Lot 정보를 반환한다`() = runTest {
            val command = RegisterLotCommand(
                "LOT-20260101-001", 1L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)
            )
            val saved: Lot = lot().lotId(1L).build()
            whenever(lotRepository.existsByProductIdAndLotNumber(1L, "LOT-20260101-001")).thenReturn(false)
            whenever(lotRepository.save(any())).thenReturn(saved)

            val result = lotService.register(command)

            assertThat(result.lotId).isEqualTo(1L)
        }

        @Test
        fun `이미 등록된 Lot 번호면 예외를 던진다`() = runTest {
            val command = RegisterLotCommand(
                "LOT-20260101-001", 1L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)
            )
            whenever(lotRepository.existsByProductIdAndLotNumber(1L, "LOT-20260101-001")).thenReturn(true)

            assertThatThrownBy { runBlocking { lotService.register(command) } }
                .isInstanceOf(DuplicateLotNumberException::class.java)
        }
    }

    @Nested
    inner class Lot_단건_조회 {

        @Test
        fun `존재하는 Lot을 조회하면 결과를 반환한다`() = runTest {
            val found: Lot = lot().lotId(1L).build()
            whenever(lotRepository.findById(1L)).thenReturn(found)

            val result = lotService.getById(1L)

            assertThat(result.lotId).isEqualTo(1L)
        }

        @Test
        fun `존재하지 않는 Lot을 조회하면 예외를 던진다`() = runTest {
            whenever(lotRepository.findById(1L)).thenReturn(null)

            assertThatThrownBy { runBlocking { lotService.getById(1L) } }
                .isInstanceOf(LotNotFoundException::class.java)
        }
    }

    @Nested
    inner class Lot_목록_조회 {

        @Test
        fun `상품 ID로 조회하면 해당 상품의 Lot 목록을 반환한다`() = runTest {
            val found: List<Lot> = listOf(lot().lotId(1L).productId(1L).build(), lot().lotId(2L).productId(1L).build())
            whenever(lotRepository.findAllByProductId(1L)).thenReturn(flowOf(*found.toTypedArray()))

            val result = lotService.getAllByProduct(1L).toList()

            assertThat(result).hasSize(2)
        }
    }
}
