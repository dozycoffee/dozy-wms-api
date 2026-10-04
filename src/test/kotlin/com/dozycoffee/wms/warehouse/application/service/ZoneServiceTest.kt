package com.dozycoffee.wms.warehouse.application.service

import com.dozycoffee.wms.warehouse.application.port.`in`.command.RegisterZoneCommand
import com.dozycoffee.wms.warehouse.application.port.out.ZoneRepository
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode
import com.dozycoffee.wms.warehouse.domain.exception.ZoneNotFoundException
import com.dozycoffee.wms.warehouse.domain.model.Zone
import com.dozycoffee.wms.warehouse.fixture.ZoneTestBuilder.Companion.zone
import com.dozycoffee.wms.support.duplicateKeyViolation
import com.dozycoffee.wms.warehouse.domain.exception.DuplicateZoneCodeException
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
class ZoneServiceTest {

    @Mock
    private lateinit var zoneRepository: ZoneRepository

    @InjectMocks
    private lateinit var zoneService: ZoneService

    @Nested
    inner class 구역_등록 {

        @Test
        fun `정상적인 정보로 등록하면 저장된 구역 정보를 반환한다`() = runTest {
            val command = RegisterZoneCommand(1L, ZoneCode.A)
            val saved: Zone = zone().zoneId(1L).build()
            whenever(zoneRepository.save(any())).thenReturn(saved)

            val result = zoneService.register(command)

            assertThat(result.zoneId).isEqualTo(1L)
            assertThat(result.zoneCode).isEqualTo(ZoneCode.A)
            assertThat(result.zoneStatus).isEqualTo(AvailabilityStatus.AVAILABLE)
        }

        @Test
        fun `같은 창고에 같은 구역 코드를 저장하면 중복 구역 코드 예외를 던진다`() = runTest {
            whenever(zoneRepository.save(any())).thenThrow(duplicateKeyViolation())

            assertThatThrownBy { runBlocking { zoneService.register(RegisterZoneCommand(1L, ZoneCode.A)) } }
                .isInstanceOf(DuplicateZoneCodeException::class.java)
        }
    }

    @Nested
    inner class 구역_단건_조회 {

        @Test
        fun `존재하는 구역을 조회하면 결과를 반환한다`() = runTest {
            val found: Zone = zone().zoneId(1L).build()
            whenever(zoneRepository.findById(1L)).thenReturn(found)

            val result = zoneService.getById(1L)

            assertThat(result.zoneId).isEqualTo(1L)
        }

        @Test
        fun `존재하지 않는 구역을 조회하면 예외를 던진다`() = runTest {
            whenever(zoneRepository.findById(1L)).thenReturn(null)

            assertThatThrownBy { runBlocking { zoneService.getById(1L) } }
                .isInstanceOf(ZoneNotFoundException::class.java)
        }
    }

    @Nested
    inner class 창고와_구역코드로_조회 {

        @Test
        fun `존재하는 구역을 조회하면 결과를 반환한다`() = runTest {
            val found: Zone = zone().zoneId(1L).warehouseId(1L).zoneCode(ZoneCode.A).build()
            whenever(zoneRepository.findByWarehouseIdAndZoneCode(1L, ZoneCode.A)).thenReturn(found)

            val result = zoneService.getByWarehouseIdAndZoneCode(1L, ZoneCode.A)

            assertThat(result.zoneId).isEqualTo(1L)
        }

        @Test
        fun `존재하지 않으면 예외를 던진다`() = runTest {
            whenever(zoneRepository.findByWarehouseIdAndZoneCode(1L, ZoneCode.A)).thenReturn(null)

            assertThatThrownBy { runBlocking { zoneService.getByWarehouseIdAndZoneCode(1L, ZoneCode.A) } }
                .isInstanceOf(ZoneNotFoundException::class.java)
        }
    }
}
