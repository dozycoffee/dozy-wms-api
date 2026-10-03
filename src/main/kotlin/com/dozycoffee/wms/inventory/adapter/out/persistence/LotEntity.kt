package com.dozycoffee.wms.inventory.adapter.out.persistence

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.inventory.domain.enumeration.LotStatus
import com.dozycoffee.wms.inventory.domain.model.Lot
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate

@Table("lot")
class LotEntity private constructor() : BaseEntity() {

    @Id
    var lotId: Long? = null
        private set

    var lotNumber: String? = null
        private set

    var productId: Long? = null
        private set

    var manufactureDate: LocalDate? = null
        private set

    var expirationDate: LocalDate? = null
        private set

    var lotStatus: String? = null
        private set

    fun toDomain(): Lot {
        return Lot.reconstitute(
            requireNotNull(lotId),
            requireNotNull(lotNumber),
            requireNotNull(productId),
            manufactureDate,
            expirationDate,
            LotStatus.valueOf(requireNotNull(lotStatus))
        )
    }

    companion object {
        fun from(domain: Lot): LotEntity {
            val entity = LotEntity()
            entity.lotId = domain.lotId
            entity.lotNumber = domain.lotNumber
            entity.productId = domain.productId
            entity.manufactureDate = domain.manufactureDate
            entity.expirationDate = domain.expirationDate
            entity.lotStatus = domain.lotStatus.name
            return entity
        }
    }
}
