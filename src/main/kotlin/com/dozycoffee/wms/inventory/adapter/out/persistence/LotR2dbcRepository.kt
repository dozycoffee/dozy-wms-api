package com.dozycoffee.wms.inventory.adapter.out.persistence

import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.time.LocalDate

interface LotR2dbcRepository : CoroutineCrudRepository<LotEntity, Long> {

    suspend fun existsByProductIdAndLotNumber(productId: Long, lotNumber: String): Boolean

    fun findAllByProductId(productId: Long): Flow<LotEntity>

    @Query(
        """
        SELECT * FROM lot
        WHERE lot_status != :lotStatus
          AND expiration_date IS NOT NULL
          AND expiration_date <= :threshold
        """
    )
    fun findAllByLotStatusNotAndExpirationDateLessThanEqual(lotStatus: String, threshold: LocalDate): Flow<LotEntity>
}
