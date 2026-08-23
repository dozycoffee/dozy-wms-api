package com.dozycoffee.wms.warehouse.domain.valueobject;

import com.dozycoffee.wms.warehouse.domain.exception.InvalidCoordinateException;
import java.math.BigDecimal;

public record Coordinate(
        BigDecimal latitude,
        BigDecimal longitude
) {
    private static final BigDecimal MIN_LATITUDE = BigDecimal.valueOf(-90);
    private static final BigDecimal MAX_LATITUDE = BigDecimal.valueOf(90);
    private static final BigDecimal MIN_LONGITUDE = BigDecimal.valueOf(-180);
    private static final BigDecimal MAX_LONGITUDE = BigDecimal.valueOf(180);

    public Coordinate {
        if (latitude == null || longitude == null) {
            throw new InvalidCoordinateException();
        }
        if (latitude.compareTo(MIN_LATITUDE) < 0 || latitude.compareTo(MAX_LATITUDE) > 0) {
            throw new InvalidCoordinateException();
        }
        if (longitude.compareTo(MIN_LONGITUDE) < 0 || longitude.compareTo(MAX_LONGITUDE) > 0) {
            throw new InvalidCoordinateException();
        }
    }

    public static Coordinate of(double latitude, double longitude) {
        return new Coordinate(BigDecimal.valueOf(latitude), BigDecimal.valueOf(longitude));
    }
}
