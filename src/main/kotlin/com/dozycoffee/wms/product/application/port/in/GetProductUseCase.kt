package com.dozycoffee.wms.product.application.port.`in`

import com.dozycoffee.wms.product.application.port.`in`.result.ProductResult

interface GetProductUseCase {
    suspend fun getById(productId: Long): ProductResult
}
