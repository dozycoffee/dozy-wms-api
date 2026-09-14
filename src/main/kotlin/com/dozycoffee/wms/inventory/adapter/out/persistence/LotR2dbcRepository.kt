package com.dozycoffee.wms.inventory.adapter.out.persistence

import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface LotR2dbcRepository : CoroutineCrudRepository<LotEntity, Long> {

    suspend fun existsByProductIdAndLotNumber(productId: Long, lotNumber: String): Boolean

    fun findAllByProductId(productId: Long): Flow<LotEntity>
}
