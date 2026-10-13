package com.dozycoffee.wms.product.adapter.out.persistence

import com.dozycoffee.wms.product.application.port.out.ProductRepository
import com.dozycoffee.wms.product.domain.enumeration.ProductCategory
import com.dozycoffee.wms.product.domain.enumeration.ProductStatus
import com.dozycoffee.wms.product.domain.model.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Component

@Component
class ProductPersistenceAdapter(
    private val productR2dbcRepository: ProductR2dbcRepository
) : ProductRepository {

    override suspend fun save(product: Product): Product {
        val existing: ProductEntity? = productR2dbcRepository.findById(product.productId)
        val entity = ProductEntity.from(product, newRow = existing == null)
        existing?.let { entity.copyAuditFieldsFrom(it) }
        return productR2dbcRepository.save(entity).toDomain()
    }

    override suspend fun findById(productId: Long): Product? {
        return productR2dbcRepository.findById(productId)?.toDomain()
    }

    override fun findAll(category: ProductCategory?, productStatus: ProductStatus?): Flow<Product> {
        return productR2dbcRepository.findAllFiltered(category?.name, productStatus?.name).map { it.toDomain() }
    }
}
