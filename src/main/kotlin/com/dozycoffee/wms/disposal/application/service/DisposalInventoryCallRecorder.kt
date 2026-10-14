package com.dozycoffee.wms.disposal.application.service

import com.dozycoffee.wms.disposal.domain.model.DisposalItem
import com.dozycoffee.wms.inventory.application.port.`in`.result.InventoryResult
import com.dozycoffee.wms.inventory_call.application.port.`in`.RecordInventoryCallUseCase
import com.dozycoffee.wms.inventory_call.application.port.`in`.command.RecordInventoryCallCommand
import com.dozycoffee.wms.inventory_call.domain.enumeration.InventoryCallType
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

/** 확정한 폐기 상품 한 줄을 inventory 폐기 확정 요청 한 건으로 아웃박스에 기록한다 (ADR-0023) */
@Component
class DisposalInventoryCallRecorder(
    private val recordInventoryCallUseCase: RecordInventoryCallUseCase,
    private val jsonMapper: JsonMapper
) {

    suspend fun record(warehouseId: Long, item: DisposalItem, inventory: InventoryResult, lotNumber: String) {
        val disposalItemId: Long = requireNotNull(item.disposalItemId)
        val body = DisposalRequestBody(
            warehouseId = warehouseId,
            productId = inventory.productId,
            lotNumber = lotNumber,
            qualityStatus = inventory.qualityStatus.name,
            quantity = item.quantity,
            disposalItemId = disposalItemId
        )
        recordInventoryCallUseCase.record(
            RecordInventoryCallCommand(
                InventoryCallType.DISPOSAL,
                warehouseId,
                "wms-disposal-$disposalItemId",
                REQUEST_PATH,
                jsonMapper.writeValueAsString(body)
            )
        )
    }

    private data class DisposalRequestBody(
        val warehouseId: Long,
        val productId: Long,
        val lotNumber: String,
        val qualityStatus: String,
        val quantity: Int,
        val disposalItemId: Long
    )

    private companion object {
        const val REQUEST_PATH: String = "/api/v1/disposals"
    }
}
