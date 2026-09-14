package com.dozycoffee.wms.product.adapter.out.persistence

import com.dozycoffee.wms.product.application.port.out.ProductRepository
import com.dozycoffee.wms.product.domain.model.Product
import org.springframework.stereotype.Component

@Component
class ProductPersistenceAdapter(
    private val productR2dbcRepository: ProductR2dbcRepository
) : ProductRepository {

    override suspend fun save(product: Product): Product {
        val entity = ProductEntity.from(product)
        val productId = product.productId
        if (productId != null) {
            productR2dbcRepository.findById(productId)?.let { entity.copyAuditFieldsFrom(it) }
        }
        return productR2dbcRepository.save(entity).toDomain()
    }

    override suspend fun findById(productId: Long): Product? {
        return productR2dbcRepository.findById(productId)?.toDomain()
    }

    override suspend fun existsByProductCode(productCode: String): Boolean {
        return productR2dbcRepository.existsByProductCode(productCode)
    }
}
