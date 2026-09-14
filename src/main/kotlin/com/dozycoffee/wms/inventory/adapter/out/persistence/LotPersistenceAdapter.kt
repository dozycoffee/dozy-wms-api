package com.dozycoffee.wms.inventory.adapter.out.persistence

import com.dozycoffee.wms.inventory.application.port.out.LotRepository
import com.dozycoffee.wms.inventory.domain.model.Lot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Component

@Component
class LotPersistenceAdapter(
    private val lotR2dbcRepository: LotR2dbcRepository
) : LotRepository {

    override suspend fun save(lot: Lot): Lot {
        val entity = LotEntity.from(lot)
        val lotId = lot.lotId
        if (lotId != null) {
            lotR2dbcRepository.findById(lotId)?.let { entity.copyAuditFieldsFrom(it) }
        }
        return lotR2dbcRepository.save(entity).toDomain()
    }

    override suspend fun findById(lotId: Long): Lot? {
        return lotR2dbcRepository.findById(lotId)?.toDomain()
    }

    override suspend fun existsByProductIdAndLotNumber(productId: Long, lotNumber: String): Boolean {
        return lotR2dbcRepository.existsByProductIdAndLotNumber(productId, lotNumber)
    }

    override fun findAllByProductId(productId: Long): Flow<Lot> {
        return lotR2dbcRepository.findAllByProductId(productId).map { it.toDomain() }
    }
}
