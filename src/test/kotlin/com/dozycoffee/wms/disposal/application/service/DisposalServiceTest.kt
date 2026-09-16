package com.dozycoffee.wms.disposal.application.service

import com.dozycoffee.wms.disposal.application.port.`in`.command.RegisterDisposalCommand
import com.dozycoffee.wms.disposal.application.port.`in`.command.RegisterDisposalItemCommand
import com.dozycoffee.wms.disposal.application.port.out.DisposalItemRepository
import com.dozycoffee.wms.disposal.application.port.out.DisposalRepository
import com.dozycoffee.wms.disposal.domain.enumeration.DisposalReason
import com.dozycoffee.wms.disposal.domain.enumeration.DisposalStatus
import com.dozycoffee.wms.disposal.domain.exception.DisposalNotFoundException
import com.dozycoffee.wms.disposal.domain.exception.DisposalQuantityMismatchException
import com.dozycoffee.wms.disposal.domain.exception.InventoryNotDisposableException
import com.dozycoffee.wms.disposal.domain.model.Disposal
import com.dozycoffee.wms.disposal.domain.model.DisposalItem
import com.dozycoffee.wms.disposal.fixture.DisposalItemTestBuilder.Companion.disposalItem
import com.dozycoffee.wms.disposal.fixture.DisposalTestBuilder.Companion.disposal
import com.dozycoffee.wms.inventory.application.port.`in`.ConfirmInventoryDisposalUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.GetInventoryUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.result.InventoryResult
import com.dozycoffee.wms.inventory.domain.enumeration.QualityStatus
import com.dozycoffee.wms.warehouse.application.port.`in`.GetWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.OccupyWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.ReleaseLocationUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.ReleaseWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.command.OccupyWorkAreaCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.command.ReleaseLocationCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.command.ReleaseWorkAreaCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.result.LocationResult
import com.dozycoffee.wms.warehouse.application.port.`in`.result.WorkAreaResult
import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import kotlinx.coroutines.flow.flowOf
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono

@ExtendWith(MockitoExtension::class)
class DisposalServiceTest {

    @Mock
    private lateinit var disposalRepository: DisposalRepository

    @Mock
    private lateinit var disposalItemRepository: DisposalItemRepository

    @Mock
    private lateinit var getInventoryUseCase: GetInventoryUseCase

    @Mock
    private lateinit var confirmInventoryDisposalUseCase: ConfirmInventoryDisposalUseCase

    @Mock
    private lateinit var getWorkAreaUseCase: GetWorkAreaUseCase

    @Mock
    private lateinit var occupyWorkAreaUseCase: OccupyWorkAreaUseCase

    @Mock
    private lateinit var releaseWorkAreaUseCase: ReleaseWorkAreaUseCase

    @Mock
    private lateinit var releaseLocationUseCase: ReleaseLocationUseCase

    @InjectMocks
    private lateinit var disposalService: DisposalService

    private fun workAreaResult(usedCapacity: Int): WorkAreaResult {
        return WorkAreaResult(
            1L, 1L, AreaCode.DISPOSAL, AreaCode.DISPOSAL.areaName, AreaCode.DISPOSAL.capacity.value, usedCapacity, AvailabilityStatus.AVAILABLE
        )
    }

    private fun locationResult(locationId: Long): LocationResult {
        return LocationResult(locationId, 10L, "A-0$locationId", 70, 30, AvailabilityStatus.AVAILABLE)
    }

    private fun inventoryResult(
        inventoryId: Long,
        quantity: Int,
        locationId: Long = 100L,
        qualityStatus: QualityStatus = QualityStatus.DISPOSAL_SCHEDULED
    ): InventoryResult {
        return InventoryResult(inventoryId, 100L, 1L, locationId, quantity, 0, quantity, qualityStatus)
    }

    @Nested
    inner class 폐기_등록 {

        @Test
        fun `대상 재고가 폐기예정 상태이고 수량이 일치하면 REQUESTED 상태로 등록된다`() = runTest {
            val command = RegisterDisposalCommand(1L, listOf(RegisterDisposalItemCommand(10L, 5, DisposalReason.EXPIRED)))
            val savedDisposal: Disposal = disposal().disposalId(1L).build()
            whenever(disposalRepository.save(any())).thenReturn(savedDisposal)
            whenever(getInventoryUseCase.getById(10L)).thenReturn(inventoryResult(10L, 5))

            val result = disposalService.register(command)

            assertThat(result.disposalId).isEqualTo(1L)
            assertThat(result.status).isEqualTo(DisposalStatus.REQUESTED)
            verify(disposalItemRepository).save(any())
        }

        @Test
        fun `대상 재고가 정상 품질이면 예외를 던진다`() = runTest {
            val command = RegisterDisposalCommand(1L, listOf(RegisterDisposalItemCommand(10L, 5, DisposalReason.EXPIRED)))
            whenever(disposalRepository.save(any())).thenReturn(disposal().disposalId(1L).build())
            whenever(getInventoryUseCase.getById(10L)).thenReturn(inventoryResult(10L, 5, qualityStatus = QualityStatus.NORMAL))

            assertThatThrownBy { runBlocking { disposalService.register(command) } }
                .isInstanceOf(InventoryNotDisposableException::class.java)
        }

        @Test
        fun `폐기 수량이 대상 재고 수량과 다르면 예외를 던진다`() = runTest {
            val command = RegisterDisposalCommand(1L, listOf(RegisterDisposalItemCommand(10L, 3, DisposalReason.EXPIRED)))
            whenever(disposalRepository.save(any())).thenReturn(disposal().disposalId(1L).build())
            whenever(getInventoryUseCase.getById(10L)).thenReturn(inventoryResult(10L, 5))

            assertThatThrownBy { runBlocking { disposalService.register(command) } }
                .isInstanceOf(DisposalQuantityMismatchException::class.java)
        }
    }

    @Nested
    inner class 폐기_승인 {

        @Test
        fun `대상 재고 수량만큼 폐기 처리장을 점유하고 APPROVED로 전환한다`() = runTest {
            val existingDisposal: Disposal = disposal().disposalId(1L).warehouseId(1L).status(DisposalStatus.REQUESTED).build()
            val item: DisposalItem = disposalItem().disposalItemId(1L).disposalId(1L).inventoryId(10L).quantity(5).build()
            whenever(disposalRepository.findById(1L)).thenReturn(existingDisposal)
            whenever(disposalItemRepository.findAllByDisposalId(1L)).thenReturn(flowOf(item))
            whenever(getWorkAreaUseCase.getByWarehouseIdAndAreaCode(1L, AreaCode.DISPOSAL)).thenReturn(Mono.just(workAreaResult(0)))
            whenever(occupyWorkAreaUseCase.occupy(any())).thenReturn(Mono.just(workAreaResult(5)))
            whenever(disposalRepository.save(any())).thenAnswer { it.getArgument(0) }

            val result = disposalService.approve(1L)

            assertThat(result.status).isEqualTo(DisposalStatus.APPROVED)
            verify(occupyWorkAreaUseCase).occupy(OccupyWorkAreaCommand(1L, 5))
        }

        @Test
        fun `존재하지 않는 폐기를 승인하면 예외를 던진다`() = runTest {
            whenever(disposalRepository.findById(1L)).thenReturn(null)

            assertThatThrownBy { runBlocking { disposalService.approve(1L) } }
                .isInstanceOf(DisposalNotFoundException::class.java)
        }
    }

    @Nested
    inner class 폐기_완료 {

        @Test
        fun `대상 재고를 soft delete하고 Location과 폐기 처리장 점유를 해제한다`() = runTest {
            val existingDisposal: Disposal = disposal().disposalId(1L).warehouseId(1L).status(DisposalStatus.APPROVED).build()
            val item: DisposalItem = disposalItem().disposalItemId(1L).disposalId(1L).inventoryId(10L).quantity(5).build()
            whenever(disposalRepository.findById(1L)).thenReturn(existingDisposal)
            whenever(disposalItemRepository.findAllByDisposalId(1L)).thenReturn(flowOf(item))
            whenever(getInventoryUseCase.getById(10L)).thenReturn(inventoryResult(10L, 5, 200L))
            whenever(releaseLocationUseCase.release(any())).thenReturn(Mono.just(locationResult(200L)))
            whenever(confirmInventoryDisposalUseCase.confirmDisposal(10L, 1L)).thenReturn(inventoryResult(10L, 0))
            whenever(getWorkAreaUseCase.getByWarehouseIdAndAreaCode(1L, AreaCode.DISPOSAL)).thenReturn(Mono.just(workAreaResult(5)))
            whenever(releaseWorkAreaUseCase.release(any())).thenReturn(Mono.just(workAreaResult(0)))
            whenever(disposalRepository.save(any())).thenAnswer { it.getArgument(0) }

            val result = disposalService.complete(1L)

            assertThat(result.status).isEqualTo(DisposalStatus.COMPLETED)
            verify(releaseLocationUseCase).release(ReleaseLocationCommand(200L, 5))
            verify(confirmInventoryDisposalUseCase).confirmDisposal(10L, 1L)
            verify(releaseWorkAreaUseCase).release(ReleaseWorkAreaCommand(1L, 5))
        }

        @Test
        fun `존재하지 않는 폐기를 완료하면 예외를 던진다`() = runTest {
            whenever(disposalRepository.findById(1L)).thenReturn(null)

            assertThatThrownBy { runBlocking { disposalService.complete(1L) } }
                .isInstanceOf(DisposalNotFoundException::class.java)
        }
    }

    @Nested
    inner class 단건_조회 {

        @Test
        fun `존재하지 않는 폐기를 조회하면 예외를 던진다`() = runTest {
            whenever(disposalRepository.findById(999L)).thenReturn(null)

            assertThatThrownBy { runBlocking { disposalService.getById(999L) } }
                .isInstanceOf(DisposalNotFoundException::class.java)
        }
    }
}
