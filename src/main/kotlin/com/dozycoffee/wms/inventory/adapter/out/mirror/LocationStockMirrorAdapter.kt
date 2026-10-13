package com.dozycoffee.wms.inventory.adapter.out.mirror

import com.dozycoffee.wms.inventory.application.port.out.LocationStockMirror
import com.dozycoffee.wms.inventory.application.port.out.LotRepository
import com.dozycoffee.wms.inventory.domain.enumeration.QualityStatus
import com.dozycoffee.wms.inventory.domain.exception.LotNotFoundException
import com.dozycoffee.wms.inventory.domain.model.Inventory
import com.dozycoffee.wms.location_stock.application.port.`in`.DecreaseLocationStockUseCase
import com.dozycoffee.wms.location_stock.application.port.`in`.IncreaseLocationStockUseCase
import com.dozycoffee.wms.location_stock.application.port.`in`.TransferLocationStockQualityUseCase
import com.dozycoffee.wms.location_stock.application.port.`in`.command.DecreaseLocationStockCommand
import com.dozycoffee.wms.location_stock.application.port.`in`.command.IncreaseLocationStockCommand
import com.dozycoffee.wms.location_stock.application.port.`in`.command.TransferLocationStockQualityCommand
import com.dozycoffee.wms.location_stock.domain.enumeration.StockQualityStatus
import org.springframework.stereotype.Component

@Component
class LocationStockMirrorAdapter(
    private val lotRepository: LotRepository,
    private val increaseLocationStockUseCase: IncreaseLocationStockUseCase,
    private val decreaseLocationStockUseCase: DecreaseLocationStockUseCase,
    private val transferLocationStockQualityUseCase: TransferLocationStockQualityUseCase
) : LocationStockMirror {

    override suspend fun increase(inventory: Inventory, quantity: Int) {
        if (quantity <= 0) return
        increaseLocationStockUseCase.increase(
            IncreaseLocationStockCommand(
                inventory.locationId,
                inventory.productId,
                lotNumberOf(inventory),
                inventory.qualityStatus.toStock(),
                quantity
            )
        )
    }

    override suspend fun decrease(inventory: Inventory, quantity: Int) {
        if (quantity <= 0) return
        decreaseLocationStockUseCase.decrease(
            DecreaseLocationStockCommand(
                inventory.locationId,
                inventory.productId,
                lotNumberOf(inventory),
                inventory.qualityStatus.toStock(),
                quantity
            )
        )
    }

    override suspend fun changeQuality(inventory: Inventory, from: QualityStatus, quantity: Int) {
        if (quantity <= 0 || from == inventory.qualityStatus) return
        transferLocationStockQualityUseCase.transferQuality(
            TransferLocationStockQualityCommand(
                inventory.locationId,
                inventory.productId,
                lotNumberOf(inventory),
                from.toStock(),
                inventory.qualityStatus.toStock(),
                quantity
            )
        )
    }

    private suspend fun lotNumberOf(inventory: Inventory): String {
        return (lotRepository.findById(inventory.lotId) ?: throw LotNotFoundException()).lotNumber
    }

    private fun QualityStatus.toStock(): StockQualityStatus = StockQualityStatus.valueOf(name)
}
