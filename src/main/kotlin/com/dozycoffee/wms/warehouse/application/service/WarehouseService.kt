package com.dozycoffee.wms.warehouse.application.service

import com.dozycoffee.wms.warehouse.application.port.`in`.ActivateWarehouseUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.DeactivateWarehouseUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.GetWarehouseUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.RegisterWarehouseUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.command.RegisterWarehouseCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.result.WarehouseResult
import com.dozycoffee.wms.warehouse.application.port.out.WarehouseRepository
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.exception.WarehouseNotFoundException
import com.dozycoffee.wms.warehouse.domain.model.Warehouse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono

@Service
class WarehouseService(
    private val warehouseRepository: WarehouseRepository
) : RegisterWarehouseUseCase, ActivateWarehouseUseCase, DeactivateWarehouseUseCase, GetWarehouseUseCase {

    @Transactional
    override fun register(command: RegisterWarehouseCommand): Mono<WarehouseResult> {
        val warehouse = Warehouse.create(
            command.warehouseName,
            command.address,
            command.latitude,
            command.longitude,
            AvailabilityStatus.AVAILABLE
        )
        return warehouseRepository.save(warehouse).map { WarehouseResult.from(it) }
    }

    @Transactional
    override fun activate(warehouseId: Long): Mono<WarehouseResult> {
        return findWarehouseOrThrow(warehouseId)
            .doOnNext { it.activate() }
            .flatMap { warehouseRepository.save(it) }
            .map { WarehouseResult.from(it) }
    }

    @Transactional
    override fun deactivate(warehouseId: Long): Mono<WarehouseResult> {
        return findWarehouseOrThrow(warehouseId)
            .doOnNext { it.deactivate() }
            .flatMap { warehouseRepository.save(it) }
            .map { WarehouseResult.from(it) }
    }

    @Transactional(readOnly = true)
    override fun getById(warehouseId: Long): Mono<WarehouseResult> {
        return findWarehouseOrThrow(warehouseId).map { WarehouseResult.from(it) }
    }

    private fun findWarehouseOrThrow(warehouseId: Long): Mono<Warehouse> {
        return warehouseRepository.findById(warehouseId)
            .switchIfEmpty(Mono.error(WarehouseNotFoundException()))
    }
}
