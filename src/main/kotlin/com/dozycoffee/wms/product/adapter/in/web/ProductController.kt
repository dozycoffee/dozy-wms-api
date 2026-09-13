package com.dozycoffee.wms.product.adapter.`in`.web

import com.dozycoffee.wms.product.adapter.`in`.web.request.RegisterProductRequest
import com.dozycoffee.wms.product.adapter.`in`.web.response.ProductResponse
import com.dozycoffee.wms.product.application.port.`in`.ActivateProductUseCase
import com.dozycoffee.wms.product.application.port.`in`.DeactivateProductUseCase
import com.dozycoffee.wms.product.application.port.`in`.GetProductUseCase
import com.dozycoffee.wms.product.application.port.`in`.RegisterProductUseCase
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/products")
class ProductController(
    private val registerProductUseCase: RegisterProductUseCase,
    private val activateProductUseCase: ActivateProductUseCase,
    private val deactivateProductUseCase: DeactivateProductUseCase,
    private val getProductUseCase: GetProductUseCase
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun register(@Valid @RequestBody request: RegisterProductRequest): ProductResponse {
        return ProductResponse.from(registerProductUseCase.register(request.toCommand()))
    }

    @GetMapping("/{productId}")
    suspend fun getById(@PathVariable productId: Long): ProductResponse {
        return ProductResponse.from(getProductUseCase.getById(productId))
    }

    @PatchMapping("/{productId}/activate")
    suspend fun activate(@PathVariable productId: Long): ProductResponse {
        return ProductResponse.from(activateProductUseCase.activate(productId))
    }

    @PatchMapping("/{productId}/deactivate")
    suspend fun deactivate(@PathVariable productId: Long): ProductResponse {
        return ProductResponse.from(deactivateProductUseCase.deactivate(productId))
    }
}
