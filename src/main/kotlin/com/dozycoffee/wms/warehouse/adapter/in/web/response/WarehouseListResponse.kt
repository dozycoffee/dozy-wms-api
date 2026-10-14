package com.dozycoffee.wms.warehouse.adapter.`in`.web.response

import com.dozycoffee.wms.warehouse.application.port.`in`.result.WarehouseResult

data class WarehouseListResponse(
    val items: List<WarehouseResponse>
) {
    companion object {
        fun from(results: List<WarehouseResult>): WarehouseListResponse {
            return WarehouseListResponse(results.map { WarehouseResponse.from(it) })
        }
    }
}
