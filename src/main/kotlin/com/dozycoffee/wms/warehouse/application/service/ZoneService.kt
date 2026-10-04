package com.dozycoffee.wms.warehouse.application.service

import com.dozycoffee.wms.global.persistence.translatingDuplicateKey
import com.dozycoffee.wms.warehouse.application.port.`in`.GetZoneUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.RegisterZoneUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.command.RegisterZoneCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.result.ZoneResult
import com.dozycoffee.wms.warehouse.application.port.out.ZoneRepository
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode
import com.dozycoffee.wms.warehouse.domain.exception.DuplicateZoneCodeException
import com.dozycoffee.wms.warehouse.domain.exception.ZoneNotFoundException
import com.dozycoffee.wms.warehouse.domain.model.Zone
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ZoneService(
    private val zoneRepository: ZoneRepository
) : RegisterZoneUseCase, GetZoneUseCase {

    @Transactional
    override suspend fun register(command: RegisterZoneCommand): ZoneResult {
        val zone = Zone.create(command.warehouseId, command.zoneCode, AvailabilityStatus.AVAILABLE)
        return ZoneResult.from(translatingDuplicateKey({ DuplicateZoneCodeException() }) { zoneRepository.save(zone) })
    }

    @Transactional(readOnly = true)
    override suspend fun getById(zoneId: Long): ZoneResult {
        val zone = zoneRepository.findById(zoneId) ?: throw ZoneNotFoundException()
        return ZoneResult.from(zone)
    }

    @Transactional(readOnly = true)
    override suspend fun getByWarehouseIdAndZoneCode(warehouseId: Long, zoneCode: ZoneCode): ZoneResult {
        val zone = zoneRepository.findByWarehouseIdAndZoneCode(warehouseId, zoneCode) ?: throw ZoneNotFoundException()
        return ZoneResult.from(zone)
    }
}
