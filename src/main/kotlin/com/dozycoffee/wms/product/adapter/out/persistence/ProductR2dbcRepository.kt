package com.dozycoffee.wms.product.adapter.out.persistence

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface ProductR2dbcRepository : CoroutineCrudRepository<ProductEntity, Long> {

    suspend fun existsByProductCode(productCode: String): Boolean
}
