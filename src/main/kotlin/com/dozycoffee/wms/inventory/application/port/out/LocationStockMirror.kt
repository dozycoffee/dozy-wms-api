package com.dozycoffee.wms.inventory.application.port.out

import com.dozycoffee.wms.inventory.domain.enumeration.QualityStatus
import com.dozycoffee.wms.inventory.domain.model.Inventory

/** 재고 변경을 Location 보조 장부에 같은 트랜잭션으로 반영한다. 각 흐름이 inventory 연동으로 전환되면 제거한다(ADR-0018) */
interface LocationStockMirror {
    suspend fun increase(inventory: Inventory, quantity: Int)

    suspend fun decrease(inventory: Inventory, quantity: Int)

    /** [inventory]는 전환 후 상태이고 [from]은 전환 전 품질 상태다 */
    suspend fun changeQuality(inventory: Inventory, from: QualityStatus, quantity: Int)
}
