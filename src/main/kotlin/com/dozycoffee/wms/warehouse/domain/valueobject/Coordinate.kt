package com.dozycoffee.wms.warehouse.domain.valueobject

import com.dozycoffee.wms.warehouse.domain.exception.InvalidCoordinateException
import java.math.BigDecimal

@ConsistentCopyVisibility
data class Coordinate private constructor(
    val latitude: BigDecimal,
    val longitude: BigDecimal
) {
    companion object {
        private val MIN_LATITUDE: BigDecimal = BigDecimal.valueOf(-90)
        private val MAX_LATITUDE: BigDecimal = BigDecimal.valueOf(90)
        private val MIN_LONGITUDE: BigDecimal = BigDecimal.valueOf(-180)
        private val MAX_LONGITUDE: BigDecimal = BigDecimal.valueOf(180)

        fun of(latitude: BigDecimal?, longitude: BigDecimal?): Coordinate {
            if (latitude == null || longitude == null) {
                throw InvalidCoordinateException()
            }
            if (latitude < MIN_LATITUDE || latitude > MAX_LATITUDE) {
                throw InvalidCoordinateException()
            }
            if (longitude < MIN_LONGITUDE || longitude > MAX_LONGITUDE) {
                throw InvalidCoordinateException()
            }
            return Coordinate(latitude, longitude)
        }

        fun of(latitude: Double, longitude: Double): Coordinate {
            return of(BigDecimal.valueOf(latitude), BigDecimal.valueOf(longitude))
        }
    }
}
