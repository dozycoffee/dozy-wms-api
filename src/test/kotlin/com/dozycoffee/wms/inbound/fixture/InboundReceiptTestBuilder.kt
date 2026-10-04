package com.dozycoffee.wms.inbound.fixture

import com.dozycoffee.wms.inbound.domain.enumeration.DefectReason
import com.dozycoffee.wms.inbound.domain.enumeration.InspectionResult
import com.dozycoffee.wms.inbound.domain.model.InboundReceipt
import java.time.LocalDate

class InboundReceiptTestBuilder {

    private var inboundItemId: Long? = 1L
    private var lotNumber: String? = "LOT-001"
    private var manufactureDate: LocalDate? = null
    private var expirationDate: LocalDate? = null
    private var quantity: Int = 10
    private var inspectionResult: InspectionResult? = InspectionResult.NORMAL
    private var defectReason: DefectReason? = null
    private var today: LocalDate = LocalDate.of(2026, 10, 4)

    companion object {
        fun inboundReceipt(): InboundReceiptTestBuilder = InboundReceiptTestBuilder()
    }

    fun inboundItemId(inboundItemId: Long?): InboundReceiptTestBuilder {
        this.inboundItemId = inboundItemId
        return this
    }

    fun lotNumber(lotNumber: String?): InboundReceiptTestBuilder {
        this.lotNumber = lotNumber
        return this
    }

    fun manufactureDate(manufactureDate: LocalDate?): InboundReceiptTestBuilder {
        this.manufactureDate = manufactureDate
        return this
    }

    fun expirationDate(expirationDate: LocalDate?): InboundReceiptTestBuilder {
        this.expirationDate = expirationDate
        return this
    }

    fun quantity(quantity: Int): InboundReceiptTestBuilder {
        this.quantity = quantity
        return this
    }

    fun normal(): InboundReceiptTestBuilder {
        this.inspectionResult = InspectionResult.NORMAL
        this.defectReason = null
        return this
    }

    fun defective(defectReason: DefectReason = DefectReason.DAMAGED): InboundReceiptTestBuilder {
        this.inspectionResult = InspectionResult.DEFECTIVE
        this.defectReason = defectReason
        return this
    }

    fun inspectionResult(inspectionResult: InspectionResult?): InboundReceiptTestBuilder {
        this.inspectionResult = inspectionResult
        return this
    }

    fun defectReason(defectReason: DefectReason?): InboundReceiptTestBuilder {
        this.defectReason = defectReason
        return this
    }

    fun today(today: LocalDate): InboundReceiptTestBuilder {
        this.today = today
        return this
    }

    fun build(): InboundReceipt {
        return InboundReceipt.create(
            inboundItemId = inboundItemId,
            lotNumber = lotNumber,
            manufactureDate = manufactureDate,
            expirationDate = expirationDate,
            quantity = quantity,
            inspectionResult = inspectionResult,
            defectReason = defectReason,
            today = today
        )
    }
}
