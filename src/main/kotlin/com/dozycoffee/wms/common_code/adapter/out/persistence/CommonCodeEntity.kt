package com.dozycoffee.wms.common_code.adapter.out.persistence

import com.dozycoffee.wms.common_code.domain.model.CommonCode
import com.dozycoffee.wms.global.common.BaseEntity
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("common_code")
class CommonCodeEntity private constructor() : BaseEntity() {

    @Id
    var code: String? = null
        private set

    var groupCode: String? = null
        private set

    var name: String? = null
        private set

    var sortOrder: Int = 0
        private set

    var active: Boolean = false
        private set

    fun toDomain(): CommonCode {
        return CommonCode.reconstruct(
            requireNotNull(code),
            requireNotNull(groupCode),
            requireNotNull(name),
            sortOrder,
            active
        )
    }

    companion object {
        fun from(domain: CommonCode): CommonCodeEntity {
            val entity = CommonCodeEntity()
            entity.code = domain.code
            entity.groupCode = domain.groupCode
            entity.name = domain.name
            entity.sortOrder = domain.sortOrder
            entity.active = domain.active
            return entity
        }
    }
}
