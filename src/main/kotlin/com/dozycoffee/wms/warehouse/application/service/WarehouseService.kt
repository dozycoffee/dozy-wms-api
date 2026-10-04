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

@Service
class WarehouseService(
    private val warehouseRepository: WarehouseRepository
) : RegisterWarehouseUseCase, ActivateWarehouseUseCase, DeactivateWarehouseUseCase, GetWarehouseUseCase {

    @Transactional
    override suspend fun register(command: RegisterWarehouseCommand): WarehouseResult {
        val warehouse = Warehouse.create(
            command.warehouseName,
            command.address,
            command.latitude,
            command.longitude,
            AvailabilityStatus.AVAILABLE
        )
        return WarehouseResult.from(warehouseRepository.save(warehouse))
    }

    @Transactional
    override suspend fun activate(warehouseId: Long): WarehouseResult {
        val warehouse = findWarehouseOrThrow(warehouseId)
        warehouse.activate()
        return WarehouseResult.from(warehouseRepository.save(warehouse))
    }

    @Transactional
    override suspend fun deactivate(warehouseId: Long): WarehouseResult {
        val warehouse = findWarehouseOrThrow(warehouseId)
        warehouse.deactivate()
        return WarehouseResult.from(warehouseRepository.save(warehouse))
    }

    @Transactional(readOnly = true)
    override suspend fun getById(warehouseId: Long): WarehouseResult {
        return WarehouseResult.from(findWarehouseOrThrow(warehouseId))
    }

    private suspend fun findWarehouseOrThrow(warehouseId: Long): Warehouse {
        return warehouseRepository.findById(warehouseId) ?: throw WarehouseNotFoundException()
    }
}
