package com.dozycoffee.wms.inbound.application.service

import com.dozycoffee.wms.inbound.domain.enumeration.InspectionResult
import com.dozycoffee.wms.inbound.domain.model.InboundReceipt
import com.dozycoffee.wms.inventory_call.application.port.`in`.RecordInventoryCallUseCase
import com.dozycoffee.wms.inventory_call.application.port.`in`.command.RecordInventoryCallCommand
import com.dozycoffee.wms.inventory_call.domain.enumeration.InventoryCallType
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate

/** 수령 라인 하나를 inventory 입고 확정 요청 한 건으로 아웃박스에 기록한다 (ADR-0023) */
@Component
class InboundInventoryCallRecorder(
    private val recordInventoryCallUseCase: RecordInventoryCallUseCase,
    private val jsonMapper: JsonMapper
) {

    suspend fun record(warehouseId: Long, productId: Long, receipt: InboundReceipt) {
        val body = InboundReceiptRequestBody(
            warehouseId = warehouseId,
            productId = productId,
            quantity = receipt.quantity,
            qualityStatus = if (receipt.inspectionResult == InspectionResult.DEFECTIVE) DEFECTIVE else NORMAL,
            lotNumber = receipt.lotNumber,
            manufactureDate = receipt.manufactureDate,
            expirationDate = receipt.expirationDate,
            inboundItemId = receipt.inboundItemId
        )
        recordInventoryCallUseCase.record(
            RecordInventoryCallCommand(
                InventoryCallType.INBOUND_RECEIPT,
                warehouseId,
                "wms-inbound-receipt-${requireNotNull(receipt.inboundReceiptId)}",
                REQUEST_PATH,
                jsonMapper.writeValueAsString(body)
            )
        )
    }

    private data class InboundReceiptRequestBody(
        val warehouseId: Long,
        val productId: Long,
        val quantity: Int,
        val qualityStatus: String,
        val lotNumber: String,
        val manufactureDate: LocalDate?,
        val expirationDate: LocalDate?,
        val inboundItemId: Long
    )

    private companion object {
        const val REQUEST_PATH: String = "/api/v1/inbound-receipts"
        const val NORMAL: String = "NORMAL"
        const val DEFECTIVE: String = "DEFECTIVE"
    }
}
