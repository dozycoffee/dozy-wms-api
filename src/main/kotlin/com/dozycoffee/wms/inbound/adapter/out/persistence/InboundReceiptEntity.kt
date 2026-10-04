package com.dozycoffee.wms.inbound.adapter.out.persistence

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.inbound.domain.enumeration.DefectReason
import com.dozycoffee.wms.inbound.domain.enumeration.InspectionResult
import com.dozycoffee.wms.inbound.domain.model.InboundReceipt
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate

@Table("inbound_receipt")
class InboundReceiptEntity private constructor() : BaseEntity() {

    @Id
    var inboundReceiptId: Long? = null
        private set

    var inboundItemId: Long? = null
        private set

    var lotNumber: String? = null
        private set

    var manufactureDate: LocalDate? = null
        private set

    var expirationDate: LocalDate? = null
        private set

    var quantity: Int = 0
        private set

    var inspectionResult: String? = null
        private set

    var defectReason: String? = null
        private set

    fun toDomain(): InboundReceipt {
        return InboundReceipt.reconstitute(
            requireNotNull(inboundReceiptId),
            requireNotNull(inboundItemId),
            requireNotNull(lotNumber),
            manufactureDate,
            expirationDate,
            quantity,
            InspectionResult.valueOf(requireNotNull(inspectionResult)),
            defectReason?.let { DefectReason.valueOf(it) }
        )
    }

    companion object {
        fun from(domain: InboundReceipt): InboundReceiptEntity {
            val entity = InboundReceiptEntity()
            entity.inboundReceiptId = domain.inboundReceiptId
            entity.inboundItemId = domain.inboundItemId
            entity.lotNumber = domain.lotNumber
            entity.manufactureDate = domain.manufactureDate
            entity.expirationDate = domain.expirationDate
            entity.quantity = domain.quantity
            entity.inspectionResult = domain.inspectionResult.name
            entity.defectReason = domain.defectReason?.name
            return entity
        }
    }
}
