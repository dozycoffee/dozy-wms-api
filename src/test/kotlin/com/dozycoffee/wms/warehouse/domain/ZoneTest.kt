package com.dozycoffee.wms.warehouse.domain

import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.enumeration.TemperatureType
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode
import com.dozycoffee.wms.warehouse.domain.exception.ZoneErrorCode
import com.dozycoffee.wms.warehouse.domain.model.Zone
import com.dozycoffee.wms.warehouse.domain.valueobject.Capacity
import com.dozycoffee.wms.warehouse.fixture.ZoneTestBuilder
import com.dozycoffee.wms.warehouse.fixture.ZoneTestBuilder.Companion.zone
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class ZoneTest {

    @Nested
    inner class 구역_생성 {

        @Test
        fun `정상적인 정보를 입력했을 때 구역 객체가 올바르게 생성된다`() {
            val zone: Zone = ZoneTestBuilder().build()

            assertThat(zone.warehouseId).isEqualTo(1L)
            assertThat(zone.zoneCode).isEqualTo(ZoneCode.A)
            assertThat(zone.zoneStatus).isEqualTo(AvailabilityStatus.AVAILABLE)
        }

        @Test
        fun `소속 창고 ID가 null인 경우 예외를 던진다`() {
            assertThatThrownBy { zone().warehouseId(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(ZoneErrorCode.INVALID_WAREHOUSE_ID.message)
        }

        @Test
        fun `구역 코드가 null인 경우 예외를 던진다`() {
            assertThatThrownBy { zone().zoneCode(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(ZoneErrorCode.INVALID_ZONE_CODE.message)
        }

        @Test
        fun `구역 상태가 null인 경우 예외를 던진다`() {
            assertThatThrownBy { zone().zoneStatus(null).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
                .hasMessage(ZoneErrorCode.INVALID_ZONE_STATUS.message)
        }
    }

    @Nested
    inner class 구역_재구성 {

        @Test
        fun `저장된 ID로 구역 객체를 재구성한다`() {
            val zone: Zone = zone().zoneId(10L).build()

            assertThat(zone.zoneId).isEqualTo(10L)
        }
    }

    @Nested
    inner class 구역_코드_속성 {

        @Test
        fun `D 구역은 냉장 온도 타입을 가진다`() {
            val zone: Zone = zone().zoneCode(ZoneCode.D).build()

            assertThat(zone.temperatureType).isEqualTo(TemperatureType.COLD)
        }

        @ParameterizedTest
        @EnumSource(value = ZoneCode::class, names = ["D"], mode = EnumSource.Mode.EXCLUDE)
        fun `D 구역을 제외한 나머지 구역은 상온 온도 타입을 가진다`(zoneCode: ZoneCode) {
            val zone: Zone = zone().zoneCode(zoneCode).build()

            assertThat(zone.temperatureType).isEqualTo(TemperatureType.AMBIENT)
        }

        @Test
        fun `구역 코드에 따라 구역명과 수용량이 함께 결정된다`() {
            val zone: Zone = zone().zoneCode(ZoneCode.A).build()

            assertThat(zone.zoneName).isEqualTo("원두")
            assertThat(zone.capacity).isEqualTo(Capacity(180))
        }
    }
}
