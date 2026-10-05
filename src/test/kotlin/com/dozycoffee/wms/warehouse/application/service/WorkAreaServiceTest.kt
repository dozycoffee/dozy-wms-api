package com.dozycoffee.wms.warehouse.application.service

import com.dozycoffee.wms.warehouse.application.port.`in`.command.OccupyWorkAreaCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.command.RegisterWorkAreaCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.command.ReleaseWorkAreaCommand
import com.dozycoffee.wms.warehouse.application.port.out.WorkAreaRepository
import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode
import com.dozycoffee.wms.warehouse.domain.exception.WorkAreaCapacityExceededException
import com.dozycoffee.wms.warehouse.domain.exception.WorkAreaNotFoundException
import com.dozycoffee.wms.warehouse.domain.model.WorkArea
import com.dozycoffee.wms.warehouse.fixture.WorkAreaTestBuilder.Companion.workArea
import com.dozycoffee.wms.support.duplicateKeyViolation
import com.dozycoffee.wms.warehouse.domain.exception.DuplicateAreaCodeException
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

@ExtendWith(MockitoExtension::class)
class WorkAreaServiceTest {

    @Mock
    private lateinit var workAreaRepository: WorkAreaRepository

    @InjectMocks
    private lateinit var workAreaService: WorkAreaService

    @Nested
    inner class 작업구역_등록 {

        @Test
        fun `정상적인 정보로 등록하면 저장된 작업구역 정보를 반환한다`() = runTest {
            val command = RegisterWorkAreaCommand(1L, AreaCode.INBOUND)
            val saved: WorkArea = workArea().workAreaId(1L).build()
            whenever(workAreaRepository.save(any())).thenReturn(saved)

            val result = workAreaService.register(command)

            assertThat(result.workAreaId).isEqualTo(1L)
            assertThat(result.areaCode).isEqualTo(AreaCode.INBOUND)
        }

        @Test
        fun `같은 창고에 같은 작업구역 코드를 저장하면 중복 작업구역 코드 예외를 던진다`() = runTest {
            whenever(workAreaRepository.save(any())).thenThrow(duplicateKeyViolation())

            assertThatThrownBy { runBlocking { workAreaService.register(RegisterWorkAreaCommand(1L, AreaCode.INBOUND)) } }
                .isInstanceOf(DuplicateAreaCodeException::class.java)
        }
    }

    @Nested
    inner class 작업구역_점유 {

        @Test
        fun `여유 용량이 있으면 점유량이 증가한다`() = runTest {
            val target: WorkArea = workArea().workAreaId(1L).usedCapacity(10).build()
            whenever(workAreaRepository.findById(1L)).thenReturn(target)
            whenever(workAreaRepository.save(any())).thenAnswer { invocation -> invocation.getArgument(0) }

            val result = workAreaService.occupy(OccupyWorkAreaCommand(1L, 5))

            assertThat(result.usedCapacity).isEqualTo(15)
        }

        @Test
        fun `최대 용량을 초과하면 예외를 던진다`() = runTest {
            val target: WorkArea = workArea().workAreaId(1L).usedCapacity(AreaCode.INBOUND.capacity.value).build()
            whenever(workAreaRepository.findById(1L)).thenReturn(target)

            assertThatThrownBy { runBlocking { workAreaService.occupy(OccupyWorkAreaCommand(1L, 1)) } }
                .isInstanceOf(WorkAreaCapacityExceededException::class.java)
        }

        @Test
        fun `존재하지 않는 작업구역을 점유하려 하면 예외를 던진다`() = runTest {
            whenever(workAreaRepository.findById(1L)).thenReturn(null)

            assertThatThrownBy { runBlocking { workAreaService.occupy(OccupyWorkAreaCommand(1L, 5)) } }
                .isInstanceOf(WorkAreaNotFoundException::class.java)
        }
    }

    @Nested
    inner class 작업구역_반출 {

        @Test
        fun `사용량 범위 내에서 반출하면 점유량이 감소한다`() = runTest {
            val target: WorkArea = workArea().workAreaId(1L).usedCapacity(10).build()
            whenever(workAreaRepository.findById(1L)).thenReturn(target)
            whenever(workAreaRepository.save(any())).thenAnswer { invocation -> invocation.getArgument(0) }

            val result = workAreaService.release(ReleaseWorkAreaCommand(1L, 4))

            assertThat(result.usedCapacity).isEqualTo(6)
        }
    }

    @Nested
    inner class 작업구역_단건_조회 {

        @Test
        fun `존재하지 않는 작업구역을 조회하면 예외를 던진다`() = runTest {
            whenever(workAreaRepository.findById(1L)).thenReturn(null)

            assertThatThrownBy { runBlocking { workAreaService.getById(1L) } }
                .isInstanceOf(WorkAreaNotFoundException::class.java)
        }
    }

    @Nested
    inner class 창고와_구역타입으로_조회 {

        @Test
        fun `존재하는 작업구역을 조회하면 결과를 반환한다`() = runTest {
            val found: WorkArea = workArea().workAreaId(1L).warehouseId(1L).areaCode(AreaCode.INBOUND).build()
            whenever(workAreaRepository.findByWarehouseIdAndAreaCode(1L, AreaCode.INBOUND)).thenReturn(found)

            val result = workAreaService.getByWarehouseIdAndAreaCode(1L, AreaCode.INBOUND)

            assertThat(result.workAreaId).isEqualTo(1L)
        }

        @Test
        fun `존재하지 않으면 예외를 던진다`() = runTest {
            whenever(workAreaRepository.findByWarehouseIdAndAreaCode(1L, AreaCode.INBOUND)).thenReturn(null)

            assertThatThrownBy { runBlocking { workAreaService.getByWarehouseIdAndAreaCode(1L, AreaCode.INBOUND) } }
                .isInstanceOf(WorkAreaNotFoundException::class.java)
        }
    }
}
