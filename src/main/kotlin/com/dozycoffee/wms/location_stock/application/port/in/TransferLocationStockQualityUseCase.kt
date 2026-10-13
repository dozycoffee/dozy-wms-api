package com.dozycoffee.wms.location_stock.application.port.`in`

import com.dozycoffee.wms.location_stock.application.port.`in`.command.TransferLocationStockQualityCommand
import com.dozycoffee.wms.location_stock.application.port.`in`.result.LocationStockResult

interface TransferLocationStockQualityUseCase {
    /** 같은 Location의 한 품질 상태 행에서 수량을 빼 다른 품질 상태 행에 더한다. 반환값은 전환 후 행이다 */
    suspend fun transferQuality(command: TransferLocationStockQualityCommand): LocationStockResult
}
