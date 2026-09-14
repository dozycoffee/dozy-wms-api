package com.dozycoffee.wms.product.adapter.out.persistence

import com.dozycoffee.wms.global.common.SoftDeletableEntity
import com.dozycoffee.wms.global.persistence.CommonCodes
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
            CommonCodes.fromCode(ProductCategory::class.java, requireNotNull(category)),
            requireNotNull(unit),
            shelfLifeDays,
            CommonCodes.fromCode(ProductStatus::class.java, requireNotNull(productStatus))
        )
    }

    companion object {
        private const val CATEGORY_GROUP = "PRODUCT_CATEGORY"
        private const val STATUS_GROUP = "PRODUCT_STATUS"

        fun from(domain: Product): ProductEntity {
            val entity = ProductEntity()
            entity.productId = domain.productId
            entity.productCode = domain.productCode
            entity.productName = domain.productName
            entity.category = CommonCodes.toCode(CATEGORY_GROUP, domain.category)
            entity.unit = domain.unit
            entity.shelfLifeDays = domain.shelfLifeDays
            entity.productStatus = CommonCodes.toCode(STATUS_GROUP, domain.productStatus)
            if (domain.isDeleted()) {
                entity.softDelete(requireNotNull(domain.deletedBy))
            }
            return entity
        }
    }
}
