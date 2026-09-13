package com.dozycoffee.wms.warehouse.fixture

import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.model.Warehouse
import com.dozycoffee.wms.warehouse.domain.valueobject.Address
import com.dozycoffee.wms.warehouse.domain.valueobject.Coordinate

class WarehouseTestBuilder {

    private var warehouseId: Long? = null
    private var warehouseName: String? = "도지하우스 제주 센터"
    private var address: Address = Address.of("제주특별자치도 제주시")
    private var coordinate: Coordinate = Coordinate.of(33.4996, 126.5312)
    private var warehouseStatus: AvailabilityStatus? = AvailabilityStatus.AVAILABLE

    companion object {
        fun warehouse(): WarehouseTestBuilder = WarehouseTestBuilder()
    }

    fun warehouseId(warehouseId: Long?): WarehouseTestBuilder {
        this.warehouseId = warehouseId
        return this
    }

    fun warehouseName(warehouseName: String?): WarehouseTestBuilder {
        this.warehouseName = warehouseName
        return this
    }

    fun address(address: String?): WarehouseTestBuilder {
        this.address = Address.of(address)
        return this
    }

    fun coordinate(coordinate: Coordinate): WarehouseTestBuilder {
        this.coordinate = coordinate
        return this
    }

    fun warehouseStatus(warehouseStatus: AvailabilityStatus?): WarehouseTestBuilder {
        this.warehouseStatus = warehouseStatus
        return this
    }

    fun build(): Warehouse {
        val id = warehouseId
        if (id != null) {
            return Warehouse.reconstitute(id, requireNotNull(warehouseName), address, coordinate, requireNotNull(warehouseStatus))
        }
        return Warehouse.create(warehouseName, address.value, coordinate.latitude, coordinate.longitude, warehouseStatus)
    }
}
