package com.dozycoffee.wms.product.adapter.`in`.web

import com.dozycoffee.wms.global.security.WmsAuthorize
import com.dozycoffee.wms.product.adapter.`in`.web.response.ProductResponse
import com.dozycoffee.wms.product.application.port.`in`.GetProductUseCase
import com.dozycoffee.wms.product.domain.enumeration.ProductCategory
import com.dozycoffee.wms.product.domain.enumeration.ProductStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** 상품 사본의 조회 API다. 상품의 등록과 상태 변경은 inventory 서비스의 `/api/v1/products`에서 한다(ADR-0021) */
@RestController
@RequestMapping("/api/products")
class ProductController(
    private val getProductUseCase: GetProductUseCase
) {

    @GetMapping
    @PreAuthorize(WmsAuthorize.READ)
    fun getAll(
        @RequestParam(required = false) category: ProductCategory?,
        @RequestParam(required = false) status: ProductStatus?
    ): Flow<ProductResponse> {
        return getProductUseCase.getAll(category, status).map { ProductResponse.from(it) }
    }

    @GetMapping("/{productId}")
    @PreAuthorize(WmsAuthorize.READ)
    suspend fun getById(@PathVariable productId: Long): ProductResponse {
        return ProductResponse.from(getProductUseCase.getById(productId))
    }
}
