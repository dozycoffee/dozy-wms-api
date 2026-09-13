package com.dozycoffee.wms.warehouse.adapter.`in`.web.response

import com.dozycoffee.wms.warehouse.application.port.`in`.result.WarehouseResult
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import java.math.BigDecimal

data class WarehouseResponse(
    val warehouseId: Long,
    val warehouseName: String,
    val address: String,
    val latitude: BigDecimal,
    val longitude: BigDecimal,
    val warehouseStatus: AvailabilityStatus
) {
    companion object {
        fun from(result: WarehouseResult): WarehouseResponse {
            return WarehouseResponse(
                result.warehouseId,
                result.warehouseName,
                result.address,
                result.latitude,
                result.longitude,
                result.warehouseStatus
            )
        }
    }
}
