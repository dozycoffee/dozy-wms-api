package com.dozycoffee.wms.product.application.port.out

import com.dozycoffee.wms.product.domain.model.Product

interface ProductRepository {
    suspend fun save(product: Product): Product
    suspend fun findById(productId: Long): Product?
}
