package com.dozycoffee.wms.warehouse.application.service

import com.dozycoffee.wms.warehouse.application.port.`in`.GetZoneUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.RegisterZoneUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.command.RegisterZoneCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.result.ZoneResult
import com.dozycoffee.wms.warehouse.application.port.out.ZoneRepository
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode
import com.dozycoffee.wms.warehouse.domain.exception.ZoneNotFoundException
import com.dozycoffee.wms.warehouse.domain.model.Zone
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono

@Service
class ZoneService(
    private val zoneRepository: ZoneRepository
) : RegisterZoneUseCase, GetZoneUseCase {

    @Transactional
    override fun register(command: RegisterZoneCommand): Mono<ZoneResult> {
        val zone = Zone.create(command.warehouseId, command.zoneCode, AvailabilityStatus.AVAILABLE)
        return zoneRepository.save(zone).map { ZoneResult.from(it) }
    }

    @Transactional(readOnly = true)
    override fun getById(zoneId: Long): Mono<ZoneResult> {
        return zoneRepository.findById(zoneId)
            .switchIfEmpty(Mono.error(ZoneNotFoundException()))
            .map { ZoneResult.from(it) }
    }

    @Transactional(readOnly = true)
    override fun getByWarehouseIdAndZoneCode(warehouseId: Long, zoneCode: ZoneCode): Mono<ZoneResult> {
        return zoneRepository.findByWarehouseIdAndZoneCode(warehouseId, zoneCode)
            .switchIfEmpty(Mono.error(ZoneNotFoundException()))
            .map { ZoneResult.from(it) }
    }
}
