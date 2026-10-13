package com.dozycoffee.wms.location_stock.application.service

import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.global.persistence.translatingDuplicateKey
import com.dozycoffee.wms.location_stock.application.port.`in`.DecreaseLocationStockUseCase
import com.dozycoffee.wms.location_stock.application.port.`in`.IncreaseLocationStockUseCase
import com.dozycoffee.wms.location_stock.application.port.`in`.TransferLocationStockQualityUseCase
import com.dozycoffee.wms.location_stock.application.port.`in`.command.DecreaseLocationStockCommand
import com.dozycoffee.wms.location_stock.application.port.`in`.command.IncreaseLocationStockCommand
import com.dozycoffee.wms.location_stock.application.port.`in`.command.TransferLocationStockQualityCommand
import com.dozycoffee.wms.location_stock.application.port.`in`.result.LocationStockResult
import com.dozycoffee.wms.location_stock.application.port.out.LocationStockRepository
import com.dozycoffee.wms.location_stock.domain.enumeration.StockQualityStatus
import com.dozycoffee.wms.location_stock.domain.exception.DuplicateLocationStockException
import com.dozycoffee.wms.location_stock.domain.exception.LocationStockErrorCode
import com.dozycoffee.wms.location_stock.domain.exception.LocationStockNotFoundException
import com.dozycoffee.wms.location_stock.domain.model.LocationStock
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LocationStockService(
    private val locationStockRepository: LocationStockRepository
) : IncreaseLocationStockUseCase,
    DecreaseLocationStockUseCase,
    TransferLocationStockQualityUseCase {

    @Transactional
    override suspend fun increase(command: IncreaseLocationStockCommand): LocationStockResult {
        return LocationStockResult.from(
            addTo(command.locationId, command.productId, command.lotNumber, command.qualityStatus, command.quantity)
        )
    }

    @Transactional
    override suspend fun decrease(command: DecreaseLocationStockCommand): LocationStockResult {
        return LocationStockResult.from(
            subtractFrom(command.locationId, command.productId, command.lotNumber, command.qualityStatus, command.quantity)
        )
    }

    @Transactional
    override suspend fun transferQuality(command: TransferLocationStockQualityCommand): LocationStockResult {
        if (command.fromQualityStatus == command.toQualityStatus) {
            throw InvalidDomainValueException(LocationStockErrorCode.SAME_QUALITY_STATUS)
        }
        subtractFrom(
            command.locationId, command.productId, command.lotNumber, command.fromQualityStatus, command.quantity
        )
        return LocationStockResult.from(
            addTo(command.locationId, command.productId, command.lotNumber, command.toQualityStatus, command.quantity)
        )
    }

    private suspend fun addTo(
        locationId: Long,
        productId: Long,
        lotNumber: String,
        qualityStatus: StockQualityStatus,
        quantity: Int
    ): LocationStock {
        val existing: LocationStock? =
            locationStockRepository.findForUpdate(locationId, productId, lotNumber, qualityStatus)
        if (existing == null) {
            val created = LocationStock.create(locationId, productId, lotNumber, qualityStatus, quantity)
            return translatingDuplicateKey({ DuplicateLocationStockException() }) {
                locationStockRepository.save(created)
            }
        }
        existing.increase(quantity)
        return locationStockRepository.save(existing)
    }

    private suspend fun subtractFrom(
        locationId: Long,
        productId: Long,
        lotNumber: String,
        qualityStatus: StockQualityStatus,
        quantity: Int
    ): LocationStock {
        val existing: LocationStock =
            locationStockRepository.findForUpdate(locationId, productId, lotNumber, qualityStatus)
                ?: throw LocationStockNotFoundException()
        existing.decrease(quantity)
        return locationStockRepository.save(existing)
    }
}
