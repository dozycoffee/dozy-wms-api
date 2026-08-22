package com.dozycoffee.wms.warehouse.domain.valueobject;

import com.dozycoffee.wms.warehouse.domain.exception.InvalidCapacityException;

public record Capacity(int value) {

    public Capacity {
        if (value < 0) {
            throw new InvalidCapacityException();
        }
    }
}
