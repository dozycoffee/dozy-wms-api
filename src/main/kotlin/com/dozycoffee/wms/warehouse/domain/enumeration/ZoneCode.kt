package com.dozycoffee.wms.warehouse.domain.enumeration

import com.dozycoffee.wms.warehouse.domain.valueobject.Capacity

enum class ZoneCode(
    val zoneName: String,
    val temperatureType: TemperatureType,
    val capacity: Capacity
) {
    A("원두", TemperatureType.AMBIENT, Capacity(820)),
    B("시럽", TemperatureType.AMBIENT, Capacity(550)),
    C("분말/파우더", TemperatureType.AMBIENT, Capacity(460)),
    D("유제품", TemperatureType.COLD, Capacity(360)),
    E("컵/소모품/포장재", TemperatureType.AMBIENT, Capacity(1710)),
    F("MD 상품", TemperatureType.AMBIENT, Capacity(420))
}
