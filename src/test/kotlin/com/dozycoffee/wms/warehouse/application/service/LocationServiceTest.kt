package com.dozycoffee.wms.warehouse.application.service

import com.dozycoffee.wms.warehouse.application.port.`in`.command.OccupyLocationCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.command.RegisterLocationCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.command.ReleaseLocationCommand
import com.dozycoffee.wms.warehouse.application.port.out.LocationRepository
import com.dozycoffee.wms.warehouse.domain.exception.InsufficientLocationCapacityException
import com.dozycoffee.wms.warehouse.domain.exception.LocationNotFoundException
import com.dozycoffee.wms.warehouse.domain.model.Location
import com.dozycoffee.wms.warehouse.fixture.LocationTestBuilder.Companion.location
import com.dozycoffee.wms.support.duplicateKeyViolation
import com.dozycoffee.wms.warehouse.domain.exception.DuplicateLocationCodeException
import kotlinx.coroutines.flow.asFlow
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

@ExtendWith(MockitoExtension::class)
class LocationServiceTest {

    @Mock
    private lateinit var locationRepository: LocationRepository

    @InjectMocks
    private lateinit var locationService: LocationService

    @Nested
    inner class 위치_등록 {

        @Test
        fun `정상적인 정보로 등록하면 저장된 위치 정보를 반환한다`() = runTest {
            val command = RegisterLocationCommand(1L, "A-01", 70)
            val saved: Location = location().locationId(1L).build()
            whenever(locationRepository.save(any())).thenReturn(saved)

            val result = locationService.register(command)

            assertThat(result.locationId).isEqualTo(1L)
            assertThat(result.locationCode).isEqualTo("A-01")
        }

        @Test
        fun `같은 구역에 같은 위치 코드를 저장하면 중복 위치 코드 예외를 던진다`() = runTest {
            whenever(locationRepository.save(any())).thenThrow(duplicateKeyViolation())

            assertThatThrownBy { runBlocking { locationService.register(RegisterLocationCommand(1L, "A-01", 70)) } }
                .isInstanceOf(DuplicateLocationCodeException::class.java)
        }
    }

    @Nested
    inner class 위치_점유 {

        @Test
        fun `여유 용량이 있으면 점유량이 증가한다`() = runTest {
            val target: Location = location().locationId(1L).usedCapacity(20).build()
            whenever(locationRepository.findById(1L)).thenReturn(target)
            whenever(locationRepository.save(any())).thenAnswer { invocation -> invocation.getArgument(0) }

            val result = locationService.occupy(OccupyLocationCommand(1L, 10))

            assertThat(result.usedCapacity).isEqualTo(30)
        }

        @Test
        fun `존재하지 않는 위치를 점유하려 하면 예외를 던진다`() = runTest {
            whenever(locationRepository.findById(1L)).thenReturn(null)

            assertThatThrownBy { runBlocking { locationService.occupy(OccupyLocationCommand(1L, 10)) } }
                .isInstanceOf(LocationNotFoundException::class.java)
        }
    }

    @Nested
    inner class 위치_반출 {

        @Test
        fun `사용량보다 많이 반출하면 예외를 던진다`() = runTest {
            val target: Location = location().locationId(1L).usedCapacity(5).build()
            whenever(locationRepository.findById(1L)).thenReturn(target)

            assertThatThrownBy { runBlocking { locationService.release(ReleaseLocationCommand(1L, 10)) } }
                .isInstanceOf(InsufficientLocationCapacityException::class.java)
        }
    }

    @Nested
    inner class 위치_단건_조회 {

        @Test
        fun `존재하는 위치를 조회하면 결과를 반환한다`() = runTest {
            val found: Location = location().locationId(1L).build()
            whenever(locationRepository.findById(1L)).thenReturn(found)

            val result = locationService.getById(1L)

            assertThat(result.locationId).isEqualTo(1L)
        }
    }

    @Nested
    inner class Zone_기준_목록_조회 {

        @Test
        fun `Zone에 속한 위치 목록을 반환한다`() = runTest {
            val locations: List<Location> = listOf(
                location().locationId(1L).zoneId(10L).locationCode("A-01").build(),
                location().locationId(2L).zoneId(10L).locationCode("A-02").build()
            )
            whenever(locationRepository.findByZoneId(10L)).thenReturn(locations.asFlow())

            val result = locationService.getByZoneId(10L).toList()

            assertThat(result.map { it.locationId }).containsExactly(1L, 2L)
        }

        @Test
        fun `Zone에 속한 위치가 없으면 빈 결과를 반환한다`() = runTest {
            whenever(locationRepository.findByZoneId(10L)).thenReturn(flowOf())

            val result = locationService.getByZoneId(10L).toList()

            assertThat(result).isEmpty()
        }
    }
}
