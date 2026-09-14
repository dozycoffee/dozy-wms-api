package com.dozycoffee.wms.product.adapter.out.persistence

import com.dozycoffee.wms.global.persistence.CommonCodes
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
        val entity = ProductEntity.from(product)
        val productId = product.productId
        if (productId != null) {
            productR2dbcRepository.findById(productId)?.let { entity.copyAuditFieldsFrom(it) }
        }
        return productR2dbcRepository.save(entity).toDomain()
    }

    override suspend fun findById(productId: Long): Product? {
        return productR2dbcRepository.findActiveById(productId)?.toDomain()
    }

    override suspend fun existsByProductCode(productCode: String): Boolean {
        return productR2dbcRepository.existsByProductCode(productCode)
    }

    override fun findAll(category: ProductCategory?, productStatus: ProductStatus?): Flow<Product> {
        val categoryCode = category?.let { CommonCodes.toCode(CATEGORY_GROUP, it) }
        val statusCode = productStatus?.let { CommonCodes.toCode(STATUS_GROUP, it) }
        return productR2dbcRepository.findAllActive(categoryCode, statusCode).map { it.toDomain() }
    }

    companion object {
        private const val CATEGORY_GROUP = "PRODUCT_CATEGORY"
        private const val STATUS_GROUP = "PRODUCT_STATUS"
    }
}
