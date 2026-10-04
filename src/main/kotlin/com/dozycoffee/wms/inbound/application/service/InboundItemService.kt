package com.dozycoffee.wms.inbound.application.service

import com.dozycoffee.wms.global.security.WarehouseAccessGuard
import com.dozycoffee.wms.inbound.application.port.`in`.GetInboundItemUseCase
import com.dozycoffee.wms.inbound.application.port.`in`.InspectInboundItemUseCase
import com.dozycoffee.wms.inbound.application.port.`in`.command.InspectInboundItemCommand
import com.dozycoffee.wms.inbound.application.port.`in`.result.InboundItemResult
import com.dozycoffee.wms.inbound.application.port.out.InboundItemRepository
import com.dozycoffee.wms.inbound.application.port.out.InboundReceiptRepository
import com.dozycoffee.wms.inbound.application.port.out.InboundRepository
import com.dozycoffee.wms.inbound.domain.exception.ExpirationDateNotAllowedException
import com.dozycoffee.wms.inbound.domain.exception.ExpirationDateRequiredException
import com.dozycoffee.wms.inbound.domain.exception.InboundItemNotFoundException
import com.dozycoffee.wms.inbound.domain.exception.InboundNotFoundException
import com.dozycoffee.wms.inbound.domain.model.InboundItem
import com.dozycoffee.wms.inbound.domain.model.InboundReceipt
import com.dozycoffee.wms.product.application.port.`in`.GetProductUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class InboundItemService(
    private val inboundItemRepository: InboundItemRepository,
    private val inboundReceiptRepository: InboundReceiptRepository,
    private val inboundRepository: InboundRepository,
    private val getProductUseCase: GetProductUseCase,
    private val inboundLotResolver: InboundLotResolver,
    private val warehouseAccessGuard: WarehouseAccessGuard
) : InspectInboundItemUseCase, GetInboundItemUseCase {

    @Transactional
    override suspend fun inspect(command: InspectInboundItemCommand): InboundItemResult {
        val inboundItem = findInboundItemOrThrow(command.inboundItemId)
        requireParentAccess(inboundItem.inboundId)

        val shelfLifeDays: Int? = getProductUseCase.getById(inboundItem.productId).shelfLifeDays
        val today: LocalDate = LocalDate.now()
        val receipts: List<InboundReceipt> = command.receipts.map {
            validateExpirationPolicy(shelfLifeDays, it.expirationDate)
            InboundReceipt.create(
                inboundItem.inboundItemId,
                it.lotNumber,
                it.manufactureDate,
                it.expirationDate,
                it.quantity,
                it.inspectionResult,
                it.defectReason,
                today
            )
        }
        receipts.map { it.lotNumber to it.expirationDate }.distinct().forEach { (lotNumber, expirationDate) ->
            inboundLotResolver.requireNoExpirationConflict(inboundItem.productId, lotNumber, expirationDate)
        }

        inboundItem.inspect(receipts)
        val savedReceipts: List<InboundReceipt> = inboundReceiptRepository.saveAll(receipts)
        return InboundItemResult.from(inboundItemRepository.save(inboundItem), savedReceipts)
    }

    @Transactional(readOnly = true)
    override fun getAllByInbound(inboundId: Long): Flow<InboundItemResult> {
        return flow {
            requireParentAccess(inboundId)
            val items: List<InboundItem> = inboundItemRepository.findAllByInboundId(inboundId).toList()
            val receiptsByItemId: Map<Long, List<InboundReceipt>> = inboundReceiptRepository
                .findAllByInboundItemIds(items.mapNotNull { it.inboundItemId })
                .groupBy { it.inboundItemId }
            items.forEach { emit(InboundItemResult.from(it, receiptsByItemId[it.inboundItemId].orEmpty())) }
        }
    }

    private fun validateExpirationPolicy(shelfLifeDays: Int?, expirationDate: LocalDate?) {
        if (shelfLifeDays != null && expirationDate == null) {
            throw ExpirationDateRequiredException()
        }
        if (shelfLifeDays == null && expirationDate != null) {
            throw ExpirationDateNotAllowedException()
        }
    }

    private suspend fun findInboundItemOrThrow(inboundItemId: Long): InboundItem {
        return inboundItemRepository.findById(inboundItemId) ?: throw InboundItemNotFoundException()
    }

    private suspend fun requireParentAccess(inboundId: Long) {
        val parent = inboundRepository.findById(inboundId) ?: throw InboundNotFoundException()
        warehouseAccessGuard.require(parent.warehouseId)
    }
}
