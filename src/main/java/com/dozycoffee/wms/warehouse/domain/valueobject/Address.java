package com.dozycoffee.wms.warehouse.domain.valueobject;

import com.dozycoffee.wms.global.error.InvalidDomainValueException;
import com.dozycoffee.wms.warehouse.domain.exception.WarehouseErrorCode;

public record Address(
        String value
) {
    public Address {
        if (value == null || value.isBlank()) {
            throw new InvalidDomainValueException(WarehouseErrorCode.INVALID_ADDRESS);
        }
    }
}
