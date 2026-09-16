package com.dozycoffee.wms.return_request.application.service

import com.dozycoffee.wms.return_request.application.port.`in`.GetReturnItemUseCase
import com.dozycoffee.wms.return_request.application.port.`in`.InspectReturnItemUseCase
import com.dozycoffee.wms.return_request.application.port.`in`.command.InspectReturnItemCommand
import com.dozycoffee.wms.return_request.application.port.`in`.result.ReturnItemResult
import com.dozycoffee.wms.return_request.application.port.out.ReturnItemRepository
import com.dozycoffee.wms.return_request.domain.exception.ReturnItemNotFoundException
import com.dozycoffee.wms.return_request.domain.model.ReturnItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReturnItemService(
    private val returnItemRepository: ReturnItemRepository
) : InspectReturnItemUseCase, GetReturnItemUseCase {

    @Transactional
    override suspend fun inspect(command: InspectReturnItemCommand): ReturnItemResult {
        val returnItem = findReturnItemOrThrow(command.returnItemId)
        returnItem.inspect(command.actualQuantity, command.inspectionResult)
        return ReturnItemResult.from(returnItemRepository.save(returnItem))
    }

    @Transactional(readOnly = true)
    override fun getAllByReturnRequest(returnRequestId: Long): Flow<ReturnItemResult> {
        return returnItemRepository.findAllByReturnRequestId(returnRequestId).map { ReturnItemResult.from(it) }
    }

    private suspend fun findReturnItemOrThrow(returnItemId: Long): ReturnItem {
        return returnItemRepository.findById(returnItemId) ?: throw ReturnItemNotFoundException()
    }
}
