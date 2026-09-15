package com.dozycoffee.wms.disposal.adapter.out.persistence

import com.dozycoffee.wms.disposal.domain.enumeration.DisposalStatus
import com.dozycoffee.wms.disposal.domain.model.Disposal
import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.global.persistence.CommonCodes
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("disposal")
class DisposalEntity private constructor() : BaseEntity() {

    @Id
    var disposalId: Long? = null
        private set

    var warehouseId: Long? = null
        private set

    var status: String? = null
        private set

    fun toDomain(): Disposal {
        return Disposal.reconstitute(
            requireNotNull(disposalId),
            requireNotNull(warehouseId),
            CommonCodes.fromCode(DisposalStatus::class.java, requireNotNull(status))
        )
    }

    companion object {
        private const val STATUS_GROUP = "DISPOSAL_STATUS"

        fun from(domain: Disposal): DisposalEntity {
            val entity = DisposalEntity()
            entity.disposalId = domain.disposalId
            entity.warehouseId = domain.warehouseId
            entity.status = CommonCodes.toCode(STATUS_GROUP, domain.status)
            return entity
        }
    }
}
