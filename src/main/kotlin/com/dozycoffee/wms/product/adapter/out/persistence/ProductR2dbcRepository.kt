package com.dozycoffee.wms.product.adapter.out.persistence

import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface ProductR2dbcRepository : CoroutineCrudRepository<ProductEntity, Long> {

    suspend fun existsByProductCode(productCode: String): Boolean

    @Query("SELECT * FROM product WHERE product_id = :productId AND deleted_at IS NULL")
    suspend fun findActiveById(productId: Long): ProductEntity?

    @Query(
        """
        SELECT * FROM product
        WHERE deleted_at IS NULL
          AND (:category IS NULL OR category = :category)
          AND (:productStatus IS NULL OR product_status = :productStatus)
        """
    )
    fun findAllActive(category: String?, productStatus: String?): Flow<ProductEntity>
}
