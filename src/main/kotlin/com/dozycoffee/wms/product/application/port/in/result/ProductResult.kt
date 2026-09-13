package com.dozycoffee.wms.product.application.port.`in`.result

import com.dozycoffee.wms.product.domain.enumeration.ProductCategory
import com.dozycoffee.wms.product.domain.enumeration.ProductStatus
import com.dozycoffee.wms.product.domain.model.Product

data class ProductResult(
    val productId: Long,
    val productCode: String,
    val productName: String,
    val category: ProductCategory,
    val unit: String,
    val shelfLifeDays: Int?,
    val productStatus: ProductStatus
) {
    companion object {
        fun from(product: Product): ProductResult {
            return ProductResult(
                requireNotNull(product.productId),
                product.productCode,
                product.productName,
                product.category,
                product.unit,
                product.shelfLifeDays,
                product.productStatus
            )
        }
    }
}
