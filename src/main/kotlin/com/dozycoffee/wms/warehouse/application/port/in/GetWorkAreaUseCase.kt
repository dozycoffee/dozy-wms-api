package com.dozycoffee.wms.warehouse.application.port.`in`

import com.dozycoffee.wms.warehouse.application.port.`in`.result.WorkAreaResult
import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode

interface GetWorkAreaUseCase {
    suspend fun getById(workAreaId: Long): WorkAreaResult
    suspend fun getByWarehouseIdAndAreaCode(warehouseId: Long, areaCode: AreaCode): WorkAreaResult
}
