package com.dozycoffee.wms.warehouse.application.port.`in`.result

import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.model.Warehouse
import java.math.BigDecimal

data class WarehouseResult(
    val warehouseId: Long,
    val warehouseName: String,
    val address: String,
    val latitude: BigDecimal,
    val longitude: BigDecimal,
    val warehouseStatus: AvailabilityStatus
) {
    companion object {
        fun from(warehouse: Warehouse): WarehouseResult {
            return WarehouseResult(
                requireNotNull(warehouse.warehouseId),
                warehouse.warehouseName,
                warehouse.address.value,
                warehouse.coordinate.latitude,
                warehouse.coordinate.longitude,
                warehouse.warehouseStatus
            )
        }
    }
}
