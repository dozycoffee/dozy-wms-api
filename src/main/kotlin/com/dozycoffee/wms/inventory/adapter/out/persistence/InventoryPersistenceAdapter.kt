package com.dozycoffee.wms.inventory.adapter.out.persistence

import com.dozycoffee.wms.global.persistence.CommonCodes
import com.dozycoffee.wms.inventory.application.port.`in`.InventorySortBy
import com.dozycoffee.wms.inventory.application.port.out.InventoryRepository
import com.dozycoffee.wms.inventory.domain.enumeration.QualityStatus
import com.dozycoffee.wms.inventory.domain.model.Inventory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Component

@Component
class InventoryPersistenceAdapter(
    private val inventoryR2dbcRepository: InventoryR2dbcRepository
) : InventoryRepository {

    override suspend fun save(inventory: Inventory): Inventory {
        val entity = InventoryEntity.from(inventory)
        val inventoryId = inventory.inventoryId
        if (inventoryId != null) {
            inventoryR2dbcRepository.findById(inventoryId)?.let { entity.copyAuditFieldsFrom(it) }
        }
        return inventoryR2dbcRepository.save(entity).toDomain()
    }

    override suspend fun findById(inventoryId: Long): Inventory? {
        return inventoryR2dbcRepository.findActiveById(inventoryId)?.toDomain()
    }

    override fun findAll(
        locationId: Long?,
        productId: Long?,
        qualityStatus: QualityStatus?,
        sortBy: InventorySortBy?
    ): Flow<Inventory> {
        val qualityStatusCode = qualityStatus?.let { CommonCodes.toCode(QUALITY_STATUS_GROUP, it) }
        return inventoryR2dbcRepository.findAllActive(locationId, productId, qualityStatusCode, sortBy?.name)
            .map { it.toDomain() }
    }

    override fun findAllByLotId(lotId: Long): Flow<Inventory> {
        return inventoryR2dbcRepository.findAllActiveByLotId(lotId).map { it.toDomain() }
    }

    companion object {
        private const val QUALITY_STATUS_GROUP = "QUALITY_STATUS"
    }
}
