package com.dozycoffee.wms.inbound.application.port.out

import com.dozycoffee.wms.inbound.domain.model.InboundReceipt

interface InboundReceiptRepository {
    suspend fun saveAll(receipts: List<InboundReceipt>): List<InboundReceipt>
    suspend fun findAllByInboundItemIds(inboundItemIds: Collection<Long>): List<InboundReceipt>
}
