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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import org.mockito.kotlin.any
import org.mockito.Mockito.`when`

@ExtendWith(MockitoExtension::class)
class WorkAreaServiceTest {

    @Mock
    private lateinit var workAreaRepository: WorkAreaRepository

    @InjectMocks
    private lateinit var workAreaService: WorkAreaService

    @Nested
    inner class 작업구역_등록 {

        @Test
        fun `정상적인 정보로 등록하면 저장된 작업구역 정보를 반환한다`() {
            val command = RegisterWorkAreaCommand(1L, AreaCode.INBOUND)
            val saved: WorkArea = workArea().workAreaId(1L).build()
            `when`(workAreaRepository.save(any())).thenReturn(Mono.just(saved))

            StepVerifier.create(workAreaService.register(command))
                .assertNext { result ->
                    assertThat(result.workAreaId).isEqualTo(1L)
                    assertThat(result.areaCode).isEqualTo(AreaCode.INBOUND)
                }
                .verifyComplete()
        }
    }

    @Nested
    inner class 작업구역_점유 {

        @Test
        fun `여유 용량이 있으면 점유량이 증가한다`() {
            val target: WorkArea = workArea().workAreaId(1L).usedCapacity(10).build()
            `when`(workAreaRepository.findById(1L)).thenReturn(Mono.just(target))
            `when`(workAreaRepository.save(any())).thenAnswer { invocation -> Mono.just(invocation.getArgument(0)) }

            StepVerifier.create(workAreaService.occupy(OccupyWorkAreaCommand(1L, 5)))
                .assertNext { result -> assertThat(result.usedCapacity).isEqualTo(15) }
                .verifyComplete()
        }

        @Test
        fun `최대 용량을 초과하면 예외를 던진다`() {
            val target: WorkArea = workArea().workAreaId(1L).usedCapacity(AreaCode.INBOUND.capacity.value).build()
            `when`(workAreaRepository.findById(1L)).thenReturn(Mono.just(target))

            StepVerifier.create(workAreaService.occupy(OccupyWorkAreaCommand(1L, 1)))
                .verifyError(WorkAreaCapacityExceededException::class.java)
        }

        @Test
        fun `존재하지 않는 작업구역을 점유하려 하면 예외를 던진다`() {
            `when`(workAreaRepository.findById(1L)).thenReturn(Mono.empty())

            StepVerifier.create(workAreaService.occupy(OccupyWorkAreaCommand(1L, 5)))
                .verifyError(WorkAreaNotFoundException::class.java)
        }
    }

    @Nested
    inner class 작업구역_반출 {

        @Test
        fun `사용량 범위 내에서 반출하면 점유량이 감소한다`() {
            val target: WorkArea = workArea().workAreaId(1L).usedCapacity(10).build()
            `when`(workAreaRepository.findById(1L)).thenReturn(Mono.just(target))
            `when`(workAreaRepository.save(any())).thenAnswer { invocation -> Mono.just(invocation.getArgument(0)) }

            StepVerifier.create(workAreaService.release(ReleaseWorkAreaCommand(1L, 4)))
                .assertNext { result -> assertThat(result.usedCapacity).isEqualTo(6) }
                .verifyComplete()
        }
    }

    @Nested
    inner class 작업구역_단건_조회 {

        @Test
        fun `존재하지 않는 작업구역을 조회하면 예외를 던진다`() {
            `when`(workAreaRepository.findById(1L)).thenReturn(Mono.empty())

            StepVerifier.create(workAreaService.getById(1L))
                .verifyError(WorkAreaNotFoundException::class.java)
        }
    }

    @Nested
    inner class 창고와_구역타입으로_조회 {

        @Test
        fun `존재하는 작업구역을 조회하면 결과를 반환한다`() {
            val found: WorkArea = workArea().workAreaId(1L).warehouseId(1L).areaCode(AreaCode.INBOUND).build()
            `when`(workAreaRepository.findByWarehouseIdAndAreaCode(1L, AreaCode.INBOUND)).thenReturn(Mono.just(found))

            StepVerifier.create(workAreaService.getByWarehouseIdAndAreaCode(1L, AreaCode.INBOUND))
                .assertNext { result -> assertThat(result.workAreaId).isEqualTo(1L) }
                .verifyComplete()
        }

        @Test
        fun `존재하지 않으면 예외를 던진다`() {
            `when`(workAreaRepository.findByWarehouseIdAndAreaCode(1L, AreaCode.INBOUND)).thenReturn(Mono.empty())

            StepVerifier.create(workAreaService.getByWarehouseIdAndAreaCode(1L, AreaCode.INBOUND))
                .verifyError(WorkAreaNotFoundException::class.java)
        }
    }
}
