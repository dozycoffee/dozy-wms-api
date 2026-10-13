package com.dozycoffee.wms.product.application.service

import com.dozycoffee.wms.product.application.port.`in`.ApplyProductSnapshotUseCase
import com.dozycoffee.wms.product.application.port.`in`.GetProductUseCase
import com.dozycoffee.wms.product.application.port.`in`.command.ApplyProductSnapshotCommand
import com.dozycoffee.wms.product.application.port.`in`.result.ProductResult
import com.dozycoffee.wms.product.application.port.out.ProductRepository
import com.dozycoffee.wms.product.domain.enumeration.ProductCategory
import com.dozycoffee.wms.product.domain.enumeration.ProductStatus
import com.dozycoffee.wms.product.domain.exception.ProductNotFoundException
import com.dozycoffee.wms.product.domain.model.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProductService(
    private val productRepository: ProductRepository
) : ApplyProductSnapshotUseCase, GetProductUseCase {

    @Transactional
    override suspend fun apply(command: ApplyProductSnapshotCommand): Boolean {
        val existing: Product? = productRepository.findById(command.productId)
        if (existing != null && existing.isStale(command.occurredAt)) {
            return false
        }
        productRepository.save(
            Product.of(
                command.productId,
                command.productCode,
                command.productName,
                command.category,
                command.unit,
                command.shelfLifeDays,
                command.productStatus,
                command.occurredAt
            )
        )
        return true
    }

    @Transactional(readOnly = true)
    override suspend fun getById(productId: Long): ProductResult {
        val product: Product = productRepository.findById(productId) ?: throw ProductNotFoundException()
        return ProductResult.from(product)
    }

    @Transactional(readOnly = true)
    override fun getAll(category: ProductCategory?, status: ProductStatus?): Flow<ProductResult> {
        return productRepository.findAll(category, status).map { ProductResult.from(it) }
    }
}
