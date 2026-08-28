package com.dozycoffee.wms.warehouse.domain.enumeration;

import com.dozycoffee.wms.warehouse.domain.valueobject.Capacity;
import lombok.Getter;

@Getter
public enum AreaCode {

    INBOUND("입고 처리장", new Capacity(50)),
    OUTBOUND("출고장", new Capacity(50)),
    RETURN("반품 처리장", new Capacity(30)),
    DISPOSAL("폐기 처리장", new Capacity(20));

    private final String areaName;
    private final Capacity capacity;

    AreaCode(String areaName, Capacity capacity) {
        this.areaName = areaName;
        this.capacity = capacity;
    }
}
