package com.dozycoffee.wms.warehouse.domain.enumeration;

import lombok.Getter;

@Getter
public enum TemperatureType {
    AMBIENT("상온"),
    COLD("냉장");

    private final String description;

    TemperatureType(String description) {
        this.description = description;
    }
}
