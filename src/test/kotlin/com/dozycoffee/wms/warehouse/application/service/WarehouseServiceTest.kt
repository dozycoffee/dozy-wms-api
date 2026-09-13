package com.dozycoffee.wms.warehouse.application.service

import com.dozycoffee.wms.warehouse.application.port.`in`.command.RegisterWarehouseCommand
import com.dozycoffee.wms.warehouse.application.port.out.WarehouseRepository
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.exception.WarehouseNotFoundException
import com.dozycoffee.wms.warehouse.domain.model.Warehouse
import com.dozycoffee.wms.warehouse.fixture.WarehouseTestBuilder.Companion.warehouse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.math.BigDecimal
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

@ExtendWith(MockitoExtension::class)
class WarehouseServiceTest {

    @Mock
    private lateinit var warehouseRepository: WarehouseRepository

    @InjectMocks
    private lateinit var warehouseService: WarehouseService

    @Nested
    inner class 창고_등록 {

        @Test
        fun `정상적인 정보로 등록하면 저장된 창고 정보를 반환한다`() {
            val command = RegisterWarehouseCommand(
                "도지하우스 제주 센터", "제주특별자치도 제주시", BigDecimal.valueOf(33.4996), BigDecimal.valueOf(126.5312)
            )
            val saved: Warehouse = warehouse().warehouseId(1L).build()
            `when`(warehouseRepository.save(any())).thenReturn(Mono.just(saved))

            StepVerifier.create(warehouseService.register(command))
                .assertNext { result ->
                    assertThat(result.warehouseId).isEqualTo(1L)
                    assertThat(result.warehouseStatus).isEqualTo(AvailabilityStatus.AVAILABLE)
                }
                .verifyComplete()

            val captor = argumentCaptor<Warehouse>()
            verify(warehouseRepository).save(captor.capture())
            assertThat(captor.firstValue.warehouseStatus).isEqualTo(AvailabilityStatus.AVAILABLE)
        }
    }

    @Nested
    inner class 창고_활성화 {

        @Test
        fun `존재하는 창고를 활성화하면 상태가 AVAILABLE로 바뀐다`() {
            val inactive: Warehouse = warehouse().warehouseId(1L).warehouseStatus(AvailabilityStatus.UNAVAILABLE).build()
            `when`(warehouseRepository.findById(1L)).thenReturn(Mono.just(inactive))
            `when`(warehouseRepository.save(any())).thenAnswer { invocation -> Mono.just(invocation.getArgument(0)) }

            StepVerifier.create(warehouseService.activate(1L))
                .assertNext { result -> assertThat(result.warehouseStatus).isEqualTo(AvailabilityStatus.AVAILABLE) }
                .verifyComplete()
        }

        @Test
        fun `존재하지 않는 창고를 활성화하면 예외를 던진다`() {
            `when`(warehouseRepository.findById(1L)).thenReturn(Mono.empty())

            StepVerifier.create(warehouseService.activate(1L))
                .verifyError(WarehouseNotFoundException::class.java)
        }
    }

    @Nested
    inner class 창고_비활성화 {

        @Test
        fun `존재하는 창고를 비활성화하면 상태가 UNAVAILABLE로 바뀐다`() {
            val active: Warehouse = warehouse().warehouseId(1L).warehouseStatus(AvailabilityStatus.AVAILABLE).build()
            `when`(warehouseRepository.findById(1L)).thenReturn(Mono.just(active))
            `when`(warehouseRepository.save(any())).thenAnswer { invocation -> Mono.just(invocation.getArgument(0)) }

            StepVerifier.create(warehouseService.deactivate(1L))
                .assertNext { result -> assertThat(result.warehouseStatus).isEqualTo(AvailabilityStatus.UNAVAILABLE) }
                .verifyComplete()
        }

        @Test
        fun `존재하지 않는 창고를 비활성화하면 예외를 던진다`() {
            `when`(warehouseRepository.findById(1L)).thenReturn(Mono.empty())

            StepVerifier.create(warehouseService.deactivate(1L))
                .verifyError(WarehouseNotFoundException::class.java)
        }
    }

    @Nested
    inner class 창고_단건_조회 {

        @Test
        fun `존재하는 창고를 조회하면 결과를 반환한다`() {
            val found: Warehouse = warehouse().warehouseId(1L).build()
            `when`(warehouseRepository.findById(1L)).thenReturn(Mono.just(found))

            StepVerifier.create(warehouseService.getById(1L))
                .assertNext { result -> assertThat(result.warehouseId).isEqualTo(1L) }
                .verifyComplete()
        }

        @Test
        fun `존재하지 않는 창고를 조회하면 예외를 던진다`() {
            `when`(warehouseRepository.findById(1L)).thenReturn(Mono.empty())

            StepVerifier.create(warehouseService.getById(1L))
                .verifyError(WarehouseNotFoundException::class.java)
        }
    }
}
