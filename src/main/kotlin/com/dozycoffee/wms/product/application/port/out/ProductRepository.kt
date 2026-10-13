package com.dozycoffee.wms.product.application.port.out

import com.dozycoffee.wms.product.domain.enumeration.ProductCategory
import com.dozycoffee.wms.product.domain.enumeration.ProductStatus
import com.dozycoffee.wms.product.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    /** 상품 ID가 이미 있으면 갱신하고 없으면 그 ID로 새로 저장한다 */
    suspend fun save(product: Product): Product
    suspend fun findById(productId: Long): Product?
    fun findAll(category: ProductCategory?, productStatus: ProductStatus?): Flow<Product>
}
