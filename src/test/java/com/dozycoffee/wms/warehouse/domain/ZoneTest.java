package com.dozycoffee.wms.warehouse.domain;

import com.dozycoffee.wms.global.error.InvalidDomainValueException;
import com.dozycoffee.wms.warehouse.domain.enumeration.TemperatureType;
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode;
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneStatus;
import com.dozycoffee.wms.warehouse.domain.exception.ZoneErrorCode;
import com.dozycoffee.wms.warehouse.domain.model.Zone;
import com.dozycoffee.wms.warehouse.domain.valueobject.Capacity;
import com.dozycoffee.wms.warehouse.fixture.ZoneTestBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static com.dozycoffee.wms.warehouse.fixture.ZoneTestBuilder.zone;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ZoneTest {

    @Nested
    class 구역_생성 {

        @Test
        void 정상적인_정보를_입력했을때_구역_객체가_올바르게_생성된다() {
            Zone zone = new ZoneTestBuilder().build();

            assertThat(zone.getWarehouseId()).isEqualTo(1L);
            assertThat(zone.getZoneCode()).isEqualTo(ZoneCode.A);
            assertThat(zone.getZoneStatus()).isEqualTo(ZoneStatus.ACTIVE);
        }

        @Test
        void 소속_창고_ID가_null인_경우_예외를_던진다() {
            assertThatThrownBy(() ->
                    zone().warehouseId(null).build()
            )
                    .isInstanceOf(InvalidDomainValueException.class)
                    .hasMessage(ZoneErrorCode.INVALID_WAREHOUSE_ID.getMessage());
        }

        @Test
        void 구역_코드가_null인_경우_예외를_던진다() {
            assertThatThrownBy(() ->
                    zone().zoneCode(null).build()
            )
                    .isInstanceOf(InvalidDomainValueException.class)
                    .hasMessage(ZoneErrorCode.INVALID_ZONE_CODE.getMessage());
        }

        @Test
        void 구역_상태가_null인_경우_예외를_던진다() {
            assertThatThrownBy(() ->
                    zone().zoneStatus(null).build()
            )
                    .isInstanceOf(InvalidDomainValueException.class)
                    .hasMessage(ZoneErrorCode.INVALID_ZONE_STATUS.getMessage());
        }
    }

    @Nested
    class 구역_재구성 {

        @Test
        void 저장된_ID로_구역_객체를_재구성한다() {
            Zone zone = zone().zoneId(10L).build();

            assertThat(zone.getZoneId()).isEqualTo(10L);
        }
    }

    @Nested
    class 구역_코드_속성 {

        @Test
        void D_구역은_냉장_온도_타입을_가진다() {
            Zone zone = zone().zoneCode(ZoneCode.D).build();

            assertThat(zone.getTemperatureType()).isEqualTo(TemperatureType.COLD);
        }

        @ParameterizedTest
        @EnumSource(value = ZoneCode.class, names = "D", mode = EnumSource.Mode.EXCLUDE)
        void D_구역을_제외한_나머지_구역은_상온_온도_타입을_가진다(ZoneCode zoneCode) {
            Zone zone = zone().zoneCode(zoneCode).build();

            assertThat(zone.getTemperatureType()).isEqualTo(TemperatureType.AMBIENT);
        }

        @Test
        void 구역_코드에_따라_구역명과_수용량이_함께_결정된다() {
            Zone zone = zone().zoneCode(ZoneCode.A).build();

            assertThat(zone.getZoneName()).isEqualTo("원두");
            assertThat(zone.getCapacity()).isEqualTo(new Capacity(180));
        }
    }
}
