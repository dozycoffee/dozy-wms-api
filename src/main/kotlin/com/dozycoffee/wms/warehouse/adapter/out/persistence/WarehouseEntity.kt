package com.dozycoffee.wms.warehouse.adapter.out.persistence

import com.dozycoffee.wms.global.common.SoftDeletableEntity
import com.dozycoffee.wms.global.persistence.CommonCodes
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.model.Warehouse
import com.dozycoffee.wms.warehouse.domain.valueobject.Address
import com.dozycoffee.wms.warehouse.domain.valueobject.Coordinate
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal

@Table("warehouse")
class WarehouseEntity private constructor() : SoftDeletableEntity() {

    @Id
    var warehouseId: Long? = null
        private set

    var warehouseName: String? = null
        private set

    var address: String? = null
        private set

    var latitude: BigDecimal? = null
        private set

    var longitude: BigDecimal? = null
        private set

    var warehouseStatus: String? = null
        private set

    fun toDomain(): Warehouse {
        return Warehouse.reconstitute(
            requireNotNull(warehouseId),
            requireNotNull(warehouseName),
            Address.of(address),
            Coordinate.of(latitude, longitude),
            CommonCodes.fromCode(AvailabilityStatus::class.java, requireNotNull(warehouseStatus))
        )
    }

    companion object {
        private const val STATUS_GROUP = "WAREHOUSE_STATUS"

        fun from(domain: Warehouse): WarehouseEntity {
            val entity = WarehouseEntity()
            entity.warehouseId = domain.warehouseId
            entity.warehouseName = domain.warehouseName
            entity.address = domain.address.value
            entity.latitude = domain.coordinate.latitude
            entity.longitude = domain.coordinate.longitude
            entity.warehouseStatus = CommonCodes.toCode(STATUS_GROUP, domain.warehouseStatus)
            return entity
        }
    }
}
