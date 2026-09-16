package com.dozycoffee.wms.return_request.adapter.out.persistence

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.global.persistence.CommonCodes
import com.dozycoffee.wms.return_request.domain.enumeration.ReturnInspectionResult
import com.dozycoffee.wms.return_request.domain.model.ReturnItem
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("return_item")
class ReturnItemEntity private constructor() : BaseEntity() {

    @Id
    var returnItemId: Long? = null
        private set

    var returnRequestId: Long? = null
        private set

    var productId: Long? = null
        private set

    var expectedQuantity: Int = 0
        private set

    var actualQuantity: Int? = null
        private set

    var inspectionResult: String? = null
        private set

    fun toDomain(): ReturnItem {
        return ReturnItem.reconstitute(
            requireNotNull(returnItemId),
            requireNotNull(returnRequestId),
            requireNotNull(productId),
            expectedQuantity,
            actualQuantity,
            CommonCodes.fromCode(ReturnInspectionResult::class.java, requireNotNull(inspectionResult))
        )
    }

    companion object {
        private const val INSPECTION_RESULT_GROUP = "RETURN_ITEM_INSPECTION_RESULT"

        fun from(domain: ReturnItem): ReturnItemEntity {
            val entity = ReturnItemEntity()
            entity.returnItemId = domain.returnItemId
            entity.returnRequestId = domain.returnRequestId
            entity.productId = domain.productId
            entity.expectedQuantity = domain.expectedQuantity
            entity.actualQuantity = domain.actualQuantity
            entity.inspectionResult = CommonCodes.toCode(INSPECTION_RESULT_GROUP, domain.inspectionResult)
            return entity
        }
    }
}
