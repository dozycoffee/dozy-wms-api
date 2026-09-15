package com.dozycoffee.wms.warehouse.application.service

import com.dozycoffee.wms.warehouse.application.port.`in`.command.OccupyLocationCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.command.RegisterLocationCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.command.ReleaseLocationCommand
import com.dozycoffee.wms.warehouse.application.port.out.LocationRepository
import com.dozycoffee.wms.warehouse.domain.exception.InsufficientLocationCapacityException
import com.dozycoffee.wms.warehouse.domain.exception.LocationNotFoundException
import com.dozycoffee.wms.warehouse.domain.model.Location
import com.dozycoffee.wms.warehouse.fixture.LocationTestBuilder.Companion.location
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import org.mockito.kotlin.any
import org.mockito.Mockito.`when`

@ExtendWith(MockitoExtension::class)
class LocationServiceTest {

    @Mock
    private lateinit var locationRepository: LocationRepository

    @InjectMocks
    private lateinit var locationService: LocationService

    @Nested
    inner class 위치_등록 {

        @Test
        fun `정상적인 정보로 등록하면 저장된 위치 정보를 반환한다`() {
            val command = RegisterLocationCommand(1L, "A-01", 70)
            val saved: Location = location().locationId(1L).build()
            `when`(locationRepository.save(any())).thenReturn(Mono.just(saved))

            StepVerifier.create(locationService.register(command))
                .assertNext { result ->
                    assertThat(result.locationId).isEqualTo(1L)
                    assertThat(result.locationCode).isEqualTo("A-01")
                }
                .verifyComplete()
        }
    }

    @Nested
    inner class 위치_점유 {

        @Test
        fun `여유 용량이 있으면 점유량이 증가한다`() {
            val target: Location = location().locationId(1L).usedCapacity(20).build()
            `when`(locationRepository.findById(1L)).thenReturn(Mono.just(target))
            `when`(locationRepository.save(any())).thenAnswer { invocation -> Mono.just(invocation.getArgument(0)) }

            StepVerifier.create(locationService.occupy(OccupyLocationCommand(1L, 10)))
                .assertNext { result -> assertThat(result.usedCapacity).isEqualTo(30) }
                .verifyComplete()
        }

        @Test
        fun `존재하지 않는 위치를 점유하려 하면 예외를 던진다`() {
            `when`(locationRepository.findById(1L)).thenReturn(Mono.empty())

            StepVerifier.create(locationService.occupy(OccupyLocationCommand(1L, 10)))
                .verifyError(LocationNotFoundException::class.java)
        }
    }

    @Nested
    inner class 위치_반출 {

        @Test
        fun `사용량보다 많이 반출하면 예외를 던진다`() {
            val target: Location = location().locationId(1L).usedCapacity(5).build()
            `when`(locationRepository.findById(1L)).thenReturn(Mono.just(target))

            StepVerifier.create(locationService.release(ReleaseLocationCommand(1L, 10)))
                .verifyError(InsufficientLocationCapacityException::class.java)
        }
    }

    @Nested
    inner class 위치_단건_조회 {

        @Test
        fun `존재하는 위치를 조회하면 결과를 반환한다`() {
            val found: Location = location().locationId(1L).build()
            `when`(locationRepository.findById(1L)).thenReturn(Mono.just(found))

            StepVerifier.create(locationService.getById(1L))
                .assertNext { result -> assertThat(result.locationId).isEqualTo(1L) }
                .verifyComplete()
        }
    }

    @Nested
    inner class Zone_기준_목록_조회 {

        @Test
        fun `Zone에 속한 위치 목록을 반환한다`() {
            val locations: List<Location> = listOf(
                location().locationId(1L).zoneId(10L).locationCode("A-01").build(),
                location().locationId(2L).zoneId(10L).locationCode("A-02").build()
            )
            `when`(locationRepository.findByZoneId(10L)).thenReturn(Flux.fromIterable(locations))

            StepVerifier.create(locationService.getByZoneId(10L))
                .expectNextMatches { it.locationId == 1L }
                .expectNextMatches { it.locationId == 2L }
                .verifyComplete()
        }

        @Test
        fun `Zone에 속한 위치가 없으면 빈 결과를 반환한다`() {
            `when`(locationRepository.findByZoneId(10L)).thenReturn(Flux.empty())

            StepVerifier.create(locationService.getByZoneId(10L))
                .verifyComplete()
        }
    }
}
