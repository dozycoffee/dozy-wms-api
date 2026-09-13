package com.dozycoffee.wms.product.adapter.`in`.web.response

import com.dozycoffee.wms.product.application.port.`in`.result.ProductResult
import com.dozycoffee.wms.product.domain.enumeration.ProductCategory
import com.dozycoffee.wms.product.domain.enumeration.ProductStatus

data class ProductResponse(
    val productId: Long,
    val productCode: String,
    val productName: String,
    val category: ProductCategory,
    val unit: String,
    val shelfLifeDays: Int?,
    val productStatus: ProductStatus
) {
    companion object {
        fun from(result: ProductResult): ProductResponse {
            return ProductResponse(
                result.productId,
                result.productCode,
                result.productName,
                result.category,
                result.unit,
                result.shelfLifeDays,
                result.productStatus
            )
        }
    }
}
