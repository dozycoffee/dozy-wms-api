package com.dozycoffee.wms.warehouse.domain.model

import com.dozycoffee.wms.global.common.SoftDeletableEntity
import com.dozycoffee.wms.global.error.DomainValidator.requireNonNull
import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.exception.WarehouseErrorCode
import com.dozycoffee.wms.warehouse.domain.valueobject.Address
import com.dozycoffee.wms.warehouse.domain.valueobject.Coordinate
import java.math.BigDecimal

class Warehouse private constructor(
    val warehouseId: Long?,
    val warehouseName: String,
    val address: Address,
    val coordinate: Coordinate,
    warehouseStatus: AvailabilityStatus
) : SoftDeletableEntity() {

    var warehouseStatus: AvailabilityStatus = warehouseStatus
        private set

    companion object {

        fun create(
            warehouseName: String?,
            address: String?,
            latitude: BigDecimal?,
            longitude: BigDecimal?,
            warehouseStatus: AvailabilityStatus?
        ): Warehouse {
            val validWarehouseName: String = validateWarehouseName(warehouseName)
            val validStatus: AvailabilityStatus =
                requireNonNull(warehouseStatus, WarehouseErrorCode.INVALID_WAREHOUSE_STATUS)
            return Warehouse(
                null,
                validWarehouseName,
                Address.of(address),
                Coordinate.of(latitude, longitude),
                validStatus
            )
        }

        fun reconstitute(
            warehouseId: Long,
            warehouseName: String,
            address: Address,
            coordinate: Coordinate,
            warehouseStatus: AvailabilityStatus
        ): Warehouse {
            return Warehouse(warehouseId, warehouseName, address, coordinate, warehouseStatus)
        }

        private fun validateWarehouseName(warehouseName: String?): String {
            if (warehouseName.isNullOrBlank()) {
                throw InvalidDomainValueException(WarehouseErrorCode.INVALID_WAREHOUSE_NAME)
            }
            return warehouseName
        }
    }

    fun activate() {
        warehouseStatus = AvailabilityStatus.AVAILABLE
    }

    fun deactivate() {
        warehouseStatus = AvailabilityStatus.UNAVAILABLE
    }
}
