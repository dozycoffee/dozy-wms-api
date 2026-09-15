package com.dozycoffee.wms.inventory.adapter.out.persistence

import com.dozycoffee.wms.global.persistence.CommonCodes
import com.dozycoffee.wms.inventory.application.port.out.AllocationRepository
import com.dozycoffee.wms.inventory.domain.enumeration.AllocationReferenceType
import com.dozycoffee.wms.inventory.domain.model.Allocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Component

@Component
class AllocationPersistenceAdapter(
    private val allocationR2dbcRepository: AllocationR2dbcRepository
) : AllocationRepository {

    override suspend fun save(allocation: Allocation): Allocation {
        val entity = AllocationEntity.from(allocation)
        val allocationId = allocation.allocationId
        if (allocationId != null) {
            allocationR2dbcRepository.findById(allocationId)?.let { entity.copyAuditFieldsFrom(it) }
        }
        return allocationR2dbcRepository.save(entity).toDomain()
    }

    override suspend fun findById(allocationId: Long): Allocation? {
        return allocationR2dbcRepository.findById(allocationId)?.toDomain()
    }

    override suspend fun findHeld(
        inventoryId: Long,
        referenceType: AllocationReferenceType,
        referenceId: Long
    ): Allocation? {
        val referenceTypeCode = CommonCodes.toCode(REFERENCE_TYPE_GROUP, referenceType)
        return allocationR2dbcRepository.findHeld(inventoryId, referenceTypeCode, referenceId)?.toDomain()
    }

    override fun findAllHeldByReference(referenceType: AllocationReferenceType, referenceId: Long): Flow<Allocation> {
        val referenceTypeCode = CommonCodes.toCode(REFERENCE_TYPE_GROUP, referenceType)
        return allocationR2dbcRepository.findAllHeldByReference(referenceTypeCode, referenceId).map { it.toDomain() }
    }

    companion object {
        private const val REFERENCE_TYPE_GROUP = "ALLOCATION_REFERENCE_TYPE"
    }
}
