package com.dozycoffee.wms.product.application.port.`in`

import com.dozycoffee.wms.product.application.port.`in`.result.ProductResult
import com.dozycoffee.wms.product.domain.enumeration.ProductCategory
import com.dozycoffee.wms.product.domain.enumeration.ProductStatus
import kotlinx.coroutines.flow.Flow

interface GetProductUseCase {
    suspend fun getById(productId: Long): ProductResult
    fun getAll(category: ProductCategory?, status: ProductStatus?): Flow<ProductResult>
}
