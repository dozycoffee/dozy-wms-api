package com.dozycoffee.wms.return_request.adapter.out.persistence

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.return_request.domain.enumeration.ReturnRequestStatus
import com.dozycoffee.wms.return_request.domain.model.ReturnRequest
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("return_request")
class ReturnRequestEntity private constructor() : BaseEntity() {

    @Id
    var returnRequestId: Long? = null
        private set

    var warehouseId: Long? = null
        private set

    var status: String? = null
        private set

    fun toDomain(): ReturnRequest {
        return ReturnRequest.reconstitute(
            requireNotNull(returnRequestId),
            requireNotNull(warehouseId),
            ReturnRequestStatus.valueOf(requireNotNull(status))
        )
    }

    companion object {
        fun from(domain: ReturnRequest): ReturnRequestEntity {
            val entity = ReturnRequestEntity()
            entity.returnRequestId = domain.returnRequestId
            entity.warehouseId = domain.warehouseId
            entity.status = domain.status.name
            return entity
        }
    }
}
