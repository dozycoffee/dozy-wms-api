package com.dozycoffee.wms.product.application.port.out

import com.dozycoffee.wms.product.domain.enumeration.ProductCategory
import com.dozycoffee.wms.product.domain.enumeration.ProductStatus
import com.dozycoffee.wms.product.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    suspend fun save(product: Product): Product
    suspend fun findById(productId: Long): Product?
    suspend fun existsByProductCode(productCode: String): Boolean
    fun findAll(category: ProductCategory?, productStatus: ProductStatus?): Flow<Product>
}
