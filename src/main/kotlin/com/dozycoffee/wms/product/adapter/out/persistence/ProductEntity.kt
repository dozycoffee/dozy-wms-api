package com.dozycoffee.wms.product.adapter.out.persistence

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.product.domain.enumeration.ProductCategory
import com.dozycoffee.wms.product.domain.enumeration.ProductStatus
import com.dozycoffee.wms.product.domain.model.Product
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

/**
 * 상품 ID는 inventory가 정한 값이라 항상 채워져 있다. 그대로 두면 Spring Data가 갱신으로 보므로 [Persistable]로 새 행 여부를 알려 준다.
 */
@Table("product")
class ProductEntity private constructor() : BaseEntity(), Persistable<Long> {

    @Id
    var productId: Long? = null
        private set

    var productCode: String? = null
        private set

    var productName: String? = null
        private set

    var category: String? = null
        private set

    var unit: String? = null
        private set

    var shelfLifeDays: Int? = null
        private set

    var productStatus: String? = null
        private set

    var sourceOccurredAt: LocalDateTime? = null
        private set

    @Transient
    private var newRow: Boolean = false

    override fun getId(): Long? = productId

    override fun isNew(): Boolean = newRow

    fun toDomain(): Product {
        return Product.reconstitute(
            requireNotNull(productId),
            requireNotNull(productCode),
            requireNotNull(productName),
            ProductCategory.valueOf(requireNotNull(category)),
            requireNotNull(unit),
            shelfLifeDays,
            ProductStatus.valueOf(requireNotNull(productStatus)),
            sourceOccurredAt
        )
    }

    companion object {
        fun from(domain: Product, newRow: Boolean): ProductEntity {
            val entity = ProductEntity()
            entity.productId = domain.productId
            entity.productCode = domain.productCode
            entity.productName = domain.productName
            entity.category = domain.category.name
            entity.unit = domain.unit
            entity.shelfLifeDays = domain.shelfLifeDays
            entity.productStatus = domain.productStatus.name
            entity.sourceOccurredAt = domain.sourceOccurredAt
            entity.newRow = newRow
            return entity
        }
    }
}
