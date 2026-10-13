package com.dozycoffee.wms.product.adapter.out.persistence

import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface ProductR2dbcRepository : CoroutineCrudRepository<ProductEntity, Long> {

    @Query(
        """
        SELECT * FROM product
        WHERE (:category IS NULL OR category = :category)
          AND (:productStatus IS NULL OR product_status = :productStatus)
        ORDER BY product_id
        """
    )
    fun findAllFiltered(category: String?, productStatus: String?): Flow<ProductEntity>
}
