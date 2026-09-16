package com.dozycoffee.wms.return_request.adapter.out.persistence

import com.dozycoffee.wms.global.persistence.CommonCodes
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

    companion object {
        private const val STATUS_GROUP = "RETURN_STATUS"
    }

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

    override fun findAll(status: ReturnRequestStatus?): Flow<ReturnRequest> {
        val statusCode = status?.let { CommonCodes.toCode(STATUS_GROUP, it) }
        return returnRequestR2dbcRepository.findAllReturnRequests(statusCode).map { it.toDomain() }
    }
}
