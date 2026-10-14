package com.dozycoffee.wms.location_stock.application.service

import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.location_stock.application.port.`in`.command.DecreaseLocationStockCommand
import com.dozycoffee.wms.location_stock.application.port.`in`.command.IncreaseLocationStockCommand
import com.dozycoffee.wms.location_stock.application.port.`in`.command.TransferLocationStockQualityCommand
import com.dozycoffee.wms.location_stock.application.port.out.LocationStockRepository
import com.dozycoffee.wms.location_stock.domain.enumeration.StockQualityStatus
import com.dozycoffee.wms.location_stock.domain.exception.DuplicateLocationStockException
import com.dozycoffee.wms.location_stock.domain.exception.InsufficientHeldLocationStockQuantityException
import com.dozycoffee.wms.location_stock.domain.exception.InsufficientLocationStockQuantityException
import com.dozycoffee.wms.location_stock.domain.exception.LocationStockNotFoundException
import com.dozycoffee.wms.location_stock.domain.model.LocationStock
import com.dozycoffee.wms.location_stock.fixture.LocationStockTestBuilder.Companion.locationStock
import com.dozycoffee.wms.support.duplicateKeyViolation
import com.dozycoffee.wms.warehouse.application.port.`in`.OccupyLocationUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.ReleaseLocationUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.command.OccupyLocationCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.command.ReleaseLocationCommand
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

    @Mock(strictness = org.mockito.Mock.Strictness.LENIENT)
    private lateinit var occupyLocationUseCase: OccupyLocationUseCase

    @Mock(strictness = org.mockito.Mock.Strictness.LENIENT)
    private lateinit var releaseLocationUseCase: ReleaseLocationUseCase

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
            verify(occupyLocationUseCase).occupy(OccupyLocationCommand(1L, 7))
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
            verify(occupyLocationUseCase).occupy(OccupyLocationCommand(1L, 7))
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

    @Nested
    inner class 감소_사용량 {

        @Test
        fun `감소하면 Location 사용량도 함께 해제한다`() = runTest {
            val existing = locationStock().locationStockId(5L).locationId(1L).productId(2L).lotNumber("L-1").quantity(10).build()
            whenever(locationStockRepository.findForUpdate(1L, 2L, "L-1", normal)).thenReturn(existing)
            whenever(locationStockRepository.save(any())).thenAnswer { it.getArgument(0) }

            locationStockService.decrease(DecreaseLocationStockCommand(1L, 2L, "L-1", normal, 4))

            verify(releaseLocationUseCase).release(ReleaseLocationCommand(1L, 4))
        }

        @Test
        fun `품질 전환은 같은 Location 안의 이동이라 사용량을 바꾸지 않는다`() = runTest {
            val from = locationStock().locationStockId(5L).locationId(1L).productId(2L).lotNumber("L-1").quantity(10).build()
            whenever(locationStockRepository.findForUpdate(1L, 2L, "L-1", normal)).thenReturn(from)
            whenever(locationStockRepository.findForUpdate(1L, 2L, "L-1", defective)).thenReturn(null)
            assignIdOnSave()

            locationStockService.transferQuality(TransferLocationStockQualityCommand(1L, 2L, "L-1", normal, defective, 4))

            verify(occupyLocationUseCase, never()).occupy(any())
            verify(releaseLocationUseCase, never()).release(any())
        }
    }

    @Nested
    inner class 출고_점유 {

        private fun row(id: Long, locationId: Long, quantity: Int, held: Int = 0): LocationStock =
            locationStock().locationStockId(id).locationId(locationId).productId(2L).lotNumber("L-1")
                .quantity(quantity).heldQuantity(held).build()

        @Test
        fun `가용 수량이 많은 행부터 필요한 수량만큼 점유한다`() = runTest {
            val big = row(1L, 10L, 8)
            val small = row(2L, 20L, 5, held = 2)
            whenever(locationStockRepository.findAvailableForUpdate(1L, 2L, "L-1", normal)).thenReturn(listOf(big, small))
            whenever(locationStockRepository.save(any())).thenAnswer { it.getArgument(0) }

            val held = locationStockService.holdForOutbound(1L, 2L, "L-1", 10)

            assertThat(held.map { Triple(it.locationStockId, it.locationId, it.quantity) })
                .containsExactly(Triple(1L, 10L, 8), Triple(2L, 20L, 2))
            assertThat(big.heldQuantity).isEqualTo(8)
            assertThat(small.heldQuantity).isEqualTo(4)
        }

        @Test
        fun `가용 수량이 모자라면 확보한 만큼만 점유한다`() = runTest {
            whenever(locationStockRepository.findAvailableForUpdate(1L, 2L, "L-1", normal)).thenReturn(listOf(row(1L, 10L, 3)))
            whenever(locationStockRepository.save(any())).thenAnswer { it.getArgument(0) }

            assertThat(locationStockService.holdForOutbound(1L, 2L, "L-1", 10).sumOf { it.quantity }).isEqualTo(3)
        }

        @Test
        fun `점유를 해제한다`() = runTest {
            val stock = row(1L, 10L, 8, held = 5)
            whenever(locationStockRepository.findByIdForUpdate(1L)).thenReturn(stock)
            whenever(locationStockRepository.save(any())).thenAnswer { it.getArgument(0) }

            locationStockService.releaseHold(1L, 3)

            assertThat(stock.heldQuantity).isEqualTo(2)
        }

        @Test
        fun `소비하면 총 수량과 점유가 줄고 Location 사용량이 해제된다`() = runTest {
            val stock = row(1L, 10L, 8, held = 5)
            whenever(locationStockRepository.findByIdForUpdate(1L)).thenReturn(stock)
            whenever(locationStockRepository.save(any())).thenAnswer { it.getArgument(0) }

            locationStockService.consume(1L, 5)

            assertThat(stock.quantity).isEqualTo(3)
            assertThat(stock.heldQuantity).isEqualTo(0)
            verify(releaseLocationUseCase).release(ReleaseLocationCommand(10L, 5))
        }

        @Test
        fun `점유가 모자라면 소비할 수 없고 행이 없으면 예외를 던진다`() = runTest {
            whenever(locationStockRepository.findByIdForUpdate(1L)).thenReturn(row(1L, 10L, 8, held = 2))
            whenever(locationStockRepository.findByIdForUpdate(9L)).thenReturn(null)

            assertThatThrownBy { runBlocking { locationStockService.consume(1L, 3) } }
                .isInstanceOf(InsufficientHeldLocationStockQuantityException::class.java)
            assertThatThrownBy { runBlocking { locationStockService.releaseHold(9L, 1) } }
                .isInstanceOf(LocationStockNotFoundException::class.java)
        }
    }

    @Nested
    inner class 폐기_예정_전환 {

        @Test
        fun `정상 행의 가용 수량을 Location별로 폐기 예정 행으로 옮긴다`() = runTest {
            val a = locationStock().locationStockId(1L).locationId(10L).productId(2L).lotNumber("L-1").quantity(8).heldQuantity(3).build()
            val b = locationStock().locationStockId(2L).locationId(20L).productId(2L).lotNumber("L-1").quantity(4).build()
            val scheduled = StockQualityStatus.DISPOSAL_SCHEDULED
            whenever(locationStockRepository.findAvailableForUpdate(1L, 2L, "L-1", normal)).thenReturn(listOf(a, b))
            whenever(locationStockRepository.findForUpdate(10L, 2L, "L-1", scheduled)).thenReturn(null)
            whenever(locationStockRepository.findForUpdate(20L, 2L, "L-1", scheduled)).thenReturn(null)
            assignIdOnSave()

            val moved = locationStockService.moveToDisposalScheduled(1L, 2L, "L-1", 7)

            assertThat(moved.map { it.locationId to it.quantity }).containsExactly(10L to 5, 20L to 2)
            assertThat(a.quantity).isEqualTo(3)
            assertThat(a.heldQuantity).isEqualTo(3)
            assertThat(b.quantity).isEqualTo(2)
        }

        @Test
        fun `정상 재고가 모자라면 옮길 수 있는 만큼만 옮긴다`() = runTest {
            val a = locationStock().locationStockId(1L).locationId(10L).productId(2L).lotNumber("L-1").quantity(3).build()
            whenever(locationStockRepository.findAvailableForUpdate(1L, 2L, "L-1", normal)).thenReturn(listOf(a))
            whenever(locationStockRepository.findForUpdate(10L, 2L, "L-1", StockQualityStatus.DISPOSAL_SCHEDULED)).thenReturn(null)
            assignIdOnSave()

            assertThat(locationStockService.moveToDisposalScheduled(1L, 2L, "L-1", 10).sumOf { it.quantity }).isEqualTo(3)
        }
    }

    @Nested
    inner class 조회 {

        @Test
        fun `행 단건·목록·Lot 존재·변경 여부를 저장소에 위임한다`() = runTest {
            val stock = locationStock().locationStockId(5L).locationId(1L).build()
            val since = java.time.LocalDateTime.of(2026, 10, 14, 10, 0)
            whenever(locationStockRepository.findById(5L)).thenReturn(stock)
            whenever(locationStockRepository.findAllByIds(listOf(5L))).thenReturn(listOf(stock))
            whenever(locationStockRepository.findAllByLocationId(1L)).thenReturn(kotlinx.coroutines.flow.flowOf(stock))
            whenever(locationStockRepository.existsByProductIdAndLotNumber(2L, "L-1")).thenReturn(true)
            whenever(locationStockRepository.existsUpdatedAfter(5L, since)).thenReturn(true)

            assertThat(locationStockService.getById(5L).locationStockId).isEqualTo(5L)
            assertThat(locationStockService.getAllByIds(listOf(5L))).hasSize(1)
            assertThat(locationStockService.getAllByLocationId(1L)).hasSize(1)
            assertThat(locationStockService.existsLot(2L, "L-1")).isTrue()
            assertThat(locationStockService.hasChangedSince(5L, since)).isTrue()
        }

        @Test
        fun `없는 행을 조회하면 예외를 던진다`() = runTest {
            whenever(locationStockRepository.findById(9L)).thenReturn(null)

            assertThatThrownBy { runBlocking { locationStockService.getById(9L) } }
                .isInstanceOf(LocationStockNotFoundException::class.java)
        }
    }
}
