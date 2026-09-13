package com.dozycoffee.wms.product.application.port.`in`

import com.dozycoffee.wms.product.application.port.`in`.result.ProductResult

interface DeactivateProductUseCase {
    suspend fun deactivate(productId: Long): ProductResult
}
