package com.dozycoffee.wms.product.adapter.out.persistence

import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface ProductR2dbcRepository : CoroutineCrudRepository<ProductEntity, Long> {

    suspend fun existsByProductCode(productCode: String): Boolean

    @Query("SELECT * FROM product WHERE product_id = :productId AND deleted_at IS NULL")
    suspend fun findActiveById(productId: Long): ProductEntity?
}
