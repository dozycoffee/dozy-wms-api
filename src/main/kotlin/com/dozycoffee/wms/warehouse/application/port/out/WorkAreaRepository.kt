package com.dozycoffee.wms.warehouse.application.port.out

import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode
import com.dozycoffee.wms.warehouse.domain.model.WorkArea

interface WorkAreaRepository {
    suspend fun save(workArea: WorkArea): WorkArea
    suspend fun findById(workAreaId: Long): WorkArea?
    suspend fun findByWarehouseIdAndAreaCode(warehouseId: Long, areaCode: AreaCode): WorkArea?
    suspend fun delete(workArea: WorkArea)
}
