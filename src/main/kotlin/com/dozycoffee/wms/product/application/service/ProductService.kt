package com.dozycoffee.wms.product.application.service

import com.dozycoffee.wms.product.application.port.`in`.ActivateProductUseCase
import com.dozycoffee.wms.product.application.port.`in`.DeactivateProductUseCase
import com.dozycoffee.wms.product.application.port.`in`.GetProductUseCase
import com.dozycoffee.wms.product.application.port.`in`.RegisterProductUseCase
import com.dozycoffee.wms.product.application.port.`in`.command.RegisterProductCommand
import com.dozycoffee.wms.product.application.port.`in`.result.ProductResult
import com.dozycoffee.wms.product.application.port.out.ProductRepository
import com.dozycoffee.wms.product.domain.exception.ProductNotFoundException
import com.dozycoffee.wms.product.domain.model.Product
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProductService(
    private val productRepository: ProductRepository
) : RegisterProductUseCase, ActivateProductUseCase, DeactivateProductUseCase, GetProductUseCase {

    @Transactional
    override suspend fun register(command: RegisterProductCommand): ProductResult {
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

    @Transactional(readOnly = true)
    override suspend fun getById(productId: Long): ProductResult {
        return ProductResult.from(findProductOrThrow(productId))
    }

    private suspend fun findProductOrThrow(productId: Long): Product {
        return productRepository.findById(productId) ?: throw ProductNotFoundException()
    }
}
