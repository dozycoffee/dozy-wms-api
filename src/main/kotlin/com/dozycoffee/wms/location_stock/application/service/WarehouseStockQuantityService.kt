package com.dozycoffee.wms.location_stock.application.service

import com.dozycoffee.wms.global.security.WarehouseAccessGuard
import com.dozycoffee.wms.location_stock.application.port.`in`.GetWarehouseStockQuantitiesUseCase
import com.dozycoffee.wms.location_stock.application.port.`in`.result.WarehouseStockQuantityResult
import com.dozycoffee.wms.location_stock.application.port.out.WarehouseStockQuantityRepository
import com.dozycoffee.wms.warehouse.application.port.`in`.GetWarehouseUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class WarehouseStockQuantityService(
    private val warehouseStockQuantityRepository: WarehouseStockQuantityRepository,
    private val getWarehouseUseCase: GetWarehouseUseCase,
    private val warehouseAccessGuard: WarehouseAccessGuard
) : GetWarehouseStockQuantitiesUseCase {

    /**
     * 없는 창고를 빈 목록으로 응답하면 호출한 쪽(inventory 대사)이 그 창고의 WMS 수량을 전부 0으로 보고 보정안을 쏟아내므로
     * 창고가 있는지 먼저 확인한다.
     */
    @Transactional(readOnly = true)
    override suspend fun getQuantities(
        warehouseId: Long,
        productId: Long?,
        lotNumber: String?
    ): List<WarehouseStockQuantityResult> {
        warehouseAccessGuard.require(warehouseId)
        getWarehouseUseCase.getById(warehouseId)
        return warehouseStockQuantityRepository.sumByWarehouse(warehouseId, productId, lotNumber)
    }
}
