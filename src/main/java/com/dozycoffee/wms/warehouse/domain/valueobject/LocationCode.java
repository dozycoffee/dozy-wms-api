package com.dozycoffee.wms.warehouse.domain.valueobject;

import com.dozycoffee.wms.warehouse.domain.exception.InvalidLocationCodeException;
import java.util.regex.Pattern;

public record LocationCode(String value) {

    private static final Pattern LOCATION_CODE_PATTERN = Pattern.compile("^[A-Z]-\\d{2}$");

    public LocationCode {
        if (value == null || !LOCATION_CODE_PATTERN.matcher(value).matches()) {
            throw new InvalidLocationCodeException();
        }
    }
}
