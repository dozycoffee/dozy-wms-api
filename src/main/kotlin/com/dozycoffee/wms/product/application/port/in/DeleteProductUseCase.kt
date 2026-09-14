package com.dozycoffee.wms.product.application.port.`in`

interface DeleteProductUseCase {
    suspend fun delete(productId: Long)
}
