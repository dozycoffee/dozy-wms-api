package com.dozycoffee.wms.product.adapter.out.persistence

import com.dozycoffee.wms.global.common.SoftDeletableEntity
import com.dozycoffee.wms.product.domain.enumeration.ProductCategory
import com.dozycoffee.wms.product.domain.enumeration.ProductStatus
import com.dozycoffee.wms.product.domain.model.Product
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("product")
class ProductEntity private constructor() : SoftDeletableEntity() {

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

    fun toDomain(): Product {
        return Product.reconstitute(
            requireNotNull(productId),
            requireNotNull(productCode),
            requireNotNull(productName),
            ProductCategory.valueOf(requireNotNull(category)),
            requireNotNull(unit),
            shelfLifeDays,
            ProductStatus.valueOf(requireNotNull(productStatus))
        )
    }

    companion object {
        fun from(domain: Product): ProductEntity {
            val entity = ProductEntity()
            entity.productId = domain.productId
            entity.productCode = domain.productCode
            entity.productName = domain.productName
            entity.category = domain.category.name
            entity.unit = domain.unit
            entity.shelfLifeDays = domain.shelfLifeDays
            entity.productStatus = domain.productStatus.name
            if (domain.isDeleted()) {
                entity.softDelete(requireNotNull(domain.deletedBy))
            }
            return entity
        }
    }
}
