package com.dozycoffee.wms.warehouse.adapter.out.persistence

import com.dozycoffee.wms.warehouse.application.port.out.ZoneRepository
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode
import com.dozycoffee.wms.warehouse.domain.model.Zone
import org.springframework.stereotype.Component

@Component
class ZonePersistenceAdapter(
    private val zoneR2dbcRepository: ZoneR2dbcRepository
) : ZoneRepository {

    override suspend fun save(zone: Zone): Zone {
        val entity = ZoneEntity.from(zone)
        val zoneId = zone.zoneId
        if (zoneId != null) {
            zoneR2dbcRepository.findById(zoneId)?.let { entity.copyAuditFieldsFrom(it) }
        }
        return zoneR2dbcRepository.save(entity).toDomain()
    }

    override suspend fun findById(zoneId: Long): Zone? {
        return zoneR2dbcRepository.findById(zoneId)?.toDomain()
    }

    override suspend fun findByWarehouseIdAndZoneCode(warehouseId: Long, zoneCode: ZoneCode): Zone? {
        return zoneR2dbcRepository.findByWarehouseIdAndZoneCode(warehouseId, zoneCode.name)?.toDomain()
    }

    override suspend fun delete(zone: Zone) {
        zoneR2dbcRepository.delete(ZoneEntity.from(zone))
    }
}
