package com.dozycoffee.wms.product.application.service

import com.dozycoffee.wms.global.security.CurrentAccessScopeProvider
import com.dozycoffee.wms.product.application.port.`in`.ActivateProductUseCase
import com.dozycoffee.wms.product.application.port.`in`.DeactivateProductUseCase
import com.dozycoffee.wms.product.application.port.`in`.DeleteProductUseCase
import com.dozycoffee.wms.product.application.port.`in`.GetProductUseCase
import com.dozycoffee.wms.product.application.port.`in`.RegisterProductUseCase
import com.dozycoffee.wms.product.application.port.`in`.command.RegisterProductCommand
import com.dozycoffee.wms.product.application.port.`in`.result.ProductResult
import com.dozycoffee.wms.product.application.port.out.ProductRepository
import com.dozycoffee.wms.product.domain.enumeration.ProductCategory
import com.dozycoffee.wms.product.domain.enumeration.ProductStatus
import com.dozycoffee.wms.product.domain.exception.DuplicateProductCodeException
import com.dozycoffee.wms.product.domain.exception.ProductNotFoundException
import com.dozycoffee.wms.product.domain.model.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val currentAccessScopeProvider: CurrentAccessScopeProvider
) : RegisterProductUseCase, ActivateProductUseCase, DeactivateProductUseCase, DeleteProductUseCase, GetProductUseCase {

    @Transactional
    override suspend fun register(command: RegisterProductCommand): ProductResult {
        if (productRepository.existsByProductCode(command.productCode)) {
            throw DuplicateProductCodeException()
        }
        val product = Product.create(
            command.productCode,
            command.productName,
            command.category,
            command.unit,
            command.shelfLifeDays
        )
        return ProductResult.from(productRepository.save(product))
    }

    @Transactional
    override suspend fun activate(productId: Long): ProductResult {
        val product = findProductOrThrow(productId)
        product.activate()
        return ProductResult.from(productRepository.save(product))
    }

    @Transactional
    override suspend fun deactivate(productId: Long): ProductResult {
        val product = findProductOrThrow(productId)
        product.deactivate()
        return ProductResult.from(productRepository.save(product))
    }

    @Transactional
    override suspend fun delete(productId: Long) {
        val product = findProductOrThrow(productId)
        product.delete(currentAccessScopeProvider.get().userId)
        productRepository.save(product)
    }

    @Transactional(readOnly = true)
    override suspend fun getById(productId: Long): ProductResult {
        return ProductResult.from(findProductOrThrow(productId))
    }

    @Transactional(readOnly = true)
    override fun getAll(category: ProductCategory?, status: ProductStatus?): Flow<ProductResult> {
        return productRepository.findAll(category, status).map { ProductResult.from(it) }
    }

    private suspend fun findProductOrThrow(productId: Long): Product {
        return productRepository.findById(productId) ?: throw ProductNotFoundException()
    }
}
