package com.dozycoffee.wms.location_stock.application.service

import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.location_stock.application.port.`in`.command.DecreaseLocationStockCommand
import com.dozycoffee.wms.location_stock.application.port.`in`.command.IncreaseLocationStockCommand
import com.dozycoffee.wms.location_stock.application.port.`in`.command.TransferLocationStockQualityCommand
import com.dozycoffee.wms.location_stock.application.port.out.LocationStockRepository
import com.dozycoffee.wms.location_stock.domain.enumeration.StockQualityStatus
import com.dozycoffee.wms.location_stock.domain.exception.DuplicateLocationStockException
import com.dozycoffee.wms.location_stock.domain.exception.InsufficientLocationStockQuantityException
import com.dozycoffee.wms.location_stock.domain.exception.LocationStockNotFoundException
import com.dozycoffee.wms.location_stock.domain.model.LocationStock
import com.dozycoffee.wms.location_stock.fixture.LocationStockTestBuilder.Companion.locationStock
import com.dozycoffee.wms.support.duplicateKeyViolation
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
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class LocationStockServiceTest {

    @Mock
    private lateinit var locationStockRepository: LocationStockRepository

    @InjectMocks
    private lateinit var locationStockService: LocationStockService

    private val normal = StockQualityStatus.NORMAL

    /** 저장소가 새 행에 식별자를 부여하는 것을 흉내 낸다 */
    private suspend fun assignIdOnSave() {
        var nextId = 100L
        whenever(locationStockRepository.save(any())).thenAnswer {
            val saved: LocationStock = it.getArgument(0)
            saved.locationStockId?.let { _ -> saved }
                ?: LocationStock.reconstitute(
                    nextId++, saved.locationId, saved.productId, saved.lotNumber, saved.qualityStatus, saved.quantity
                )
        }
    }
    private val defective = StockQualityStatus.DEFECTIVE

    @Nested
    inner class 증가 {

        @Test
        fun `같은 키의 행이 없으면 새로 만든다`() = runTest {
            whenever(locationStockRepository.findForUpdate(1L, 2L, "L-1", normal)).thenReturn(null)
            assignIdOnSave()

            val result = locationStockService.increase(IncreaseLocationStockCommand(1L, 2L, "L-1", normal, 7))

            assertThat(result.quantity).isEqualTo(7)
            val captor = argumentCaptor<LocationStock>()
            verify(locationStockRepository).save(captor.capture())
            assertThat(captor.firstValue.locationId).isEqualTo(1L)
            assertThat(captor.firstValue.lotNumber).isEqualTo("L-1")
        }

        @Test
        fun `같은 키의 행이 있으면 수량을 더한다`() = runTest {
            val existing = locationStock().locationStockId(5L).locationId(1L).productId(2L).lotNumber("L-1").quantity(10).build()
            whenever(locationStockRepository.findForUpdate(1L, 2L, "L-1", normal)).thenReturn(existing)
            whenever(locationStockRepository.save(any())).thenAnswer { it.getArgument(0) }

            val result = locationStockService.increase(IncreaseLocationStockCommand(1L, 2L, "L-1", normal, 7))

            assertThat(result.locationStockId).isEqualTo(5L)
            assertThat(result.quantity).isEqualTo(17)
        }

        @Test
        fun `동시 생성으로 유니크 제약에 걸리면 중복 예외로 바꾼다`() = runTest {
            whenever(locationStockRepository.findForUpdate(1L, 2L, "L-1", normal)).thenReturn(null)
            whenever(locationStockRepository.save(any())).thenThrow(duplicateKeyViolation())

            assertThatThrownBy {
                runBlocking { locationStockService.increase(IncreaseLocationStockCommand(1L, 2L, "L-1", normal, 7)) }
            }.isInstanceOf(DuplicateLocationStockException::class.java)
        }
    }

    @Nested
    inner class 감소 {

        @Test
        fun `행이 있으면 수량을 뺀다`() = runTest {
            val existing = locationStock().locationStockId(5L).locationId(1L).productId(2L).lotNumber("L-1").quantity(10).build()
            whenever(locationStockRepository.findForUpdate(1L, 2L, "L-1", normal)).thenReturn(existing)
            whenever(locationStockRepository.save(any())).thenAnswer { it.getArgument(0) }

            val result = locationStockService.decrease(DecreaseLocationStockCommand(1L, 2L, "L-1", normal, 4))

            assertThat(result.quantity).isEqualTo(6)
        }

        @Test
        fun `행이 없으면 예외를 던진다`() = runTest {
            whenever(locationStockRepository.findForUpdate(1L, 2L, "L-1", normal)).thenReturn(null)

            assertThatThrownBy {
                runBlocking { locationStockService.decrease(DecreaseLocationStockCommand(1L, 2L, "L-1", normal, 4)) }
            }.isInstanceOf(LocationStockNotFoundException::class.java)
        }

        @Test
        fun `보유 수량보다 많이 빼면 저장하지 않고 예외를 던진다`() = runTest {
            val existing = locationStock().locationStockId(5L).locationId(1L).productId(2L).lotNumber("L-1").quantity(3).build()
            whenever(locationStockRepository.findForUpdate(1L, 2L, "L-1", normal)).thenReturn(existing)

            assertThatThrownBy {
                runBlocking { locationStockService.decrease(DecreaseLocationStockCommand(1L, 2L, "L-1", normal, 4)) }
            }.isInstanceOf(InsufficientLocationStockQuantityException::class.java)
            verify(locationStockRepository, never()).save(any())
        }
    }

    @Nested
    inner class 품질_상태_전환 {

        @Test
        fun `원래 행에서 빼고 전환 후 행에 더한다`() = runTest {
            val from = locationStock().locationStockId(5L).locationId(1L).productId(2L).lotNumber("L-1").quantity(10).build()
            whenever(locationStockRepository.findForUpdate(1L, 2L, "L-1", normal)).thenReturn(from)
            whenever(locationStockRepository.findForUpdate(1L, 2L, "L-1", defective)).thenReturn(null)
            assignIdOnSave()

            val result = locationStockService.transferQuality(
                TransferLocationStockQualityCommand(1L, 2L, "L-1", normal, defective, 4)
            )

            assertThat(result.qualityStatus).isEqualTo(defective)
            assertThat(result.quantity).isEqualTo(4)
            assertThat(from.quantity).isEqualTo(6)
            verify(locationStockRepository, times(2)).save(any())
        }

        @Test
        fun `전환 후 행이 이미 있으면 수량을 합친다`() = runTest {
            val from = locationStock().locationStockId(5L).locationId(1L).productId(2L).lotNumber("L-1").quantity(10).build()
            val to = locationStock().locationStockId(6L).locationId(1L).productId(2L).lotNumber("L-1")
                .qualityStatus(defective).quantity(3).build()
            whenever(locationStockRepository.findForUpdate(1L, 2L, "L-1", normal)).thenReturn(from)
            whenever(locationStockRepository.findForUpdate(1L, 2L, "L-1", defective)).thenReturn(to)
            whenever(locationStockRepository.save(any())).thenAnswer { it.getArgument(0) }

            val result = locationStockService.transferQuality(
                TransferLocationStockQualityCommand(1L, 2L, "L-1", normal, defective, 10)
            )

            assertThat(result.quantity).isEqualTo(13)
            assertThat(from.quantity).isEqualTo(0)
        }

        @Test
        fun `전환 전후 품질 상태가 같으면 예외를 던진다`() = runTest {
            assertThatThrownBy {
                runBlocking {
                    locationStockService.transferQuality(
                        TransferLocationStockQualityCommand(1L, 2L, "L-1", normal, normal, 4)
                    )
                }
            }.isInstanceOf(InvalidDomainValueException::class.java)
        }

        @Test
        fun `원래 행의 수량이 부족하면 전환 후 행을 건드리지 않는다`() = runTest {
            val from = locationStock().locationStockId(5L).locationId(1L).productId(2L).lotNumber("L-1").quantity(2).build()
            whenever(locationStockRepository.findForUpdate(1L, 2L, "L-1", normal)).thenReturn(from)

            assertThatThrownBy {
                runBlocking {
                    locationStockService.transferQuality(
                        TransferLocationStockQualityCommand(1L, 2L, "L-1", normal, defective, 4)
                    )
                }
            }.isInstanceOf(InsufficientLocationStockQuantityException::class.java)
            verify(locationStockRepository, never()).findForUpdate(1L, 2L, "L-1", defective)
        }
    }
}
