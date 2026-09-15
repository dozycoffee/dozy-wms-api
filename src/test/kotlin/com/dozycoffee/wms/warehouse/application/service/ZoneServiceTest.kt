package com.dozycoffee.wms.warehouse.application.service

import com.dozycoffee.wms.warehouse.application.port.`in`.command.RegisterZoneCommand
import com.dozycoffee.wms.warehouse.application.port.out.ZoneRepository
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode
import com.dozycoffee.wms.warehouse.domain.exception.ZoneNotFoundException
import com.dozycoffee.wms.warehouse.domain.model.Zone
import com.dozycoffee.wms.warehouse.fixture.ZoneTestBuilder.Companion.zone
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
class ZoneServiceTest {

    @Mock
    private lateinit var zoneRepository: ZoneRepository

    @InjectMocks
    private lateinit var zoneService: ZoneService

    @Nested
    inner class 구역_등록 {

        @Test
        fun `정상적인 정보로 등록하면 저장된 구역 정보를 반환한다`() {
            val command = RegisterZoneCommand(1L, ZoneCode.A)
            val saved: Zone = zone().zoneId(1L).build()
            `when`(zoneRepository.save(any())).thenReturn(Mono.just(saved))

            StepVerifier.create(zoneService.register(command))
                .assertNext { result ->
                    assertThat(result.zoneId).isEqualTo(1L)
                    assertThat(result.zoneCode).isEqualTo(ZoneCode.A)
                    assertThat(result.zoneStatus).isEqualTo(AvailabilityStatus.AVAILABLE)
                }
                .verifyComplete()
        }
    }

    @Nested
    inner class 구역_단건_조회 {

        @Test
        fun `존재하는 구역을 조회하면 결과를 반환한다`() {
            val found: Zone = zone().zoneId(1L).build()
            `when`(zoneRepository.findById(1L)).thenReturn(Mono.just(found))

            StepVerifier.create(zoneService.getById(1L))
                .assertNext { result -> assertThat(result.zoneId).isEqualTo(1L) }
                .verifyComplete()
        }

        @Test
        fun `존재하지 않는 구역을 조회하면 예외를 던진다`() {
            `when`(zoneRepository.findById(1L)).thenReturn(Mono.empty())

            StepVerifier.create(zoneService.getById(1L))
                .verifyError(ZoneNotFoundException::class.java)
        }
    }

    @Nested
    inner class 창고와_구역코드로_조회 {

        @Test
        fun `존재하는 구역을 조회하면 결과를 반환한다`() {
            val found: Zone = zone().zoneId(1L).warehouseId(1L).zoneCode(ZoneCode.A).build()
            `when`(zoneRepository.findByWarehouseIdAndZoneCode(1L, ZoneCode.A)).thenReturn(Mono.just(found))

            StepVerifier.create(zoneService.getByWarehouseIdAndZoneCode(1L, ZoneCode.A))
                .assertNext { result -> assertThat(result.zoneId).isEqualTo(1L) }
                .verifyComplete()
        }

        @Test
        fun `존재하지 않으면 예외를 던진다`() {
            `when`(zoneRepository.findByWarehouseIdAndZoneCode(1L, ZoneCode.A)).thenReturn(Mono.empty())

            StepVerifier.create(zoneService.getByWarehouseIdAndZoneCode(1L, ZoneCode.A))
                .verifyError(ZoneNotFoundException::class.java)
        }
    }
}
