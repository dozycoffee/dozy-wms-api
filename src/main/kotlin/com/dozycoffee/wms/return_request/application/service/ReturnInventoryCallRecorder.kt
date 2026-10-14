package com.dozycoffee.wms.return_request.application.service

import com.dozycoffee.wms.inventory_call.application.port.`in`.RecordInventoryCallUseCase
import com.dozycoffee.wms.inventory_call.application.port.`in`.command.RecordInventoryCallCommand
import com.dozycoffee.wms.inventory_call.domain.enumeration.InventoryCallType
import com.dozycoffee.wms.return_request.domain.enumeration.ReturnInspectionResult
import com.dozycoffee.wms.return_request.domain.model.ReturnItem
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

/** 검수 완료한 반품 상품 한 줄을 inventory 반품 확정 요청 한 건으로 아웃박스에 기록한다 (ADR-0023) */
@Component
class ReturnInventoryCallRecorder(
    private val recordInventoryCallUseCase: RecordInventoryCallUseCase,
    private val jsonMapper: JsonMapper
) {

    suspend fun record(warehouseId: Long, item: ReturnItem, lotNumber: String) {
        val returnItemId: Long = requireNotNull(item.returnItemId)
        val body = ReturnRequestBody(
            warehouseId = warehouseId,
            productId = item.productId,
            lotNumber = lotNumber,
            qualityStatus = if (item.inspectionResult == ReturnInspectionResult.DEFECTIVE) DEFECTIVE else NORMAL,
            quantity = requireNotNull(item.actualQuantity),
            returnItemId = returnItemId
        )
        recordInventoryCallUseCase.record(
            RecordInventoryCallCommand(
                InventoryCallType.RETURN,
                warehouseId,
                "wms-return-$returnItemId",
                REQUEST_PATH,
                jsonMapper.writeValueAsString(body)
            )
        )
    }

    private data class ReturnRequestBody(
        val warehouseId: Long,
        val productId: Long,
        val lotNumber: String,
        val qualityStatus: String,
        val quantity: Int,
        val returnItemId: Long
    )

    private companion object {
        const val REQUEST_PATH: String = "/api/v1/returns"
        const val NORMAL: String = "NORMAL"
        const val DEFECTIVE: String = "DEFECTIVE"
    }
}
