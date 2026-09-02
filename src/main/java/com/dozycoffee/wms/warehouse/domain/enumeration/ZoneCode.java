package com.dozycoffee.wms.warehouse.domain.enumeration;

import com.dozycoffee.wms.warehouse.domain.valueobject.Capacity;
import lombok.Getter;

@Getter
public enum ZoneCode {

    A("원두", TemperatureType.AMBIENT, new Capacity(180)),
    B("시럽", TemperatureType.AMBIENT, new Capacity(120)),
    C("분말/파우더", TemperatureType.AMBIENT, new Capacity(100)),
    D("유제품", TemperatureType.COLD, new Capacity(80)),
    E("컵/소모품/포장재", TemperatureType.AMBIENT, new Capacity(370));

    private final String zoneName;
    private final TemperatureType temperatureType;
    private final Capacity capacity;

    ZoneCode(String zoneName, TemperatureType temperatureType, Capacity capacity) {
        this.zoneName = zoneName;
        this.temperatureType = temperatureType;
        this.capacity = capacity;
    }
}
