package com.dozycoffee.wms.product.application.port.`in`

import com.dozycoffee.wms.product.application.port.`in`.result.ProductResult

interface ActivateProductUseCase {
    suspend fun activate(productId: Long): ProductResult
}
