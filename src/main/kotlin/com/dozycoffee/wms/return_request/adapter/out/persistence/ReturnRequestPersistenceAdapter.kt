package com.dozycoffee.wms.return_request.adapter.out.persistence

import com.dozycoffee.wms.return_request.application.port.out.ReturnRequestRepository
import com.dozycoffee.wms.return_request.domain.enumeration.ReturnRequestStatus
import com.dozycoffee.wms.return_request.domain.model.ReturnRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Component

@Component
class ReturnRequestPersistenceAdapter(
    private val returnRequestR2dbcRepository: ReturnRequestR2dbcRepository
) : ReturnRequestRepository {

    override suspend fun save(returnRequest: ReturnRequest): ReturnRequest {
        val entity = ReturnRequestEntity.from(returnRequest)
        val returnRequestId = returnRequest.returnRequestId
        if (returnRequestId != null) {
            returnRequestR2dbcRepository.findById(returnRequestId)?.let { entity.copyAuditFieldsFrom(it) }
        }
        return returnRequestR2dbcRepository.save(entity).toDomain()
    }

    override suspend fun findById(returnRequestId: Long): ReturnRequest? {
        return returnRequestR2dbcRepository.findById(returnRequestId)?.toDomain()
    }

    override fun findAll(status: ReturnRequestStatus?, warehouseIds: List<Long>?): Flow<ReturnRequest> {
        val statusCode = status?.name
        val ids: List<Long>? = warehouseIds?.takeIf { it.isNotEmpty() }
        val entities = if (ids == null) {
            returnRequestR2dbcRepository.findAllReturnRequests(statusCode)
        } else {
            returnRequestR2dbcRepository.findAllReturnRequestsInWarehouses(statusCode, ids)
        }
        return entities.map { it.toDomain() }
    }
}
