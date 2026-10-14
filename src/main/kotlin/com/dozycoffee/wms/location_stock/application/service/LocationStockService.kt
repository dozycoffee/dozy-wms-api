package com.dozycoffee.wms.location_stock.application.service

import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.global.persistence.translatingDuplicateKey
import com.dozycoffee.wms.location_stock.application.port.`in`.DecreaseLocationStockUseCase
import com.dozycoffee.wms.location_stock.application.port.`in`.GetLocationStockUseCase
import com.dozycoffee.wms.location_stock.application.port.`in`.HoldLocationStockUseCase
import com.dozycoffee.wms.location_stock.application.port.`in`.IncreaseLocationStockUseCase
import com.dozycoffee.wms.location_stock.application.port.`in`.ScheduleLocationStockDisposalUseCase
import com.dozycoffee.wms.location_stock.application.port.`in`.TransferLocationStockQualityUseCase
import com.dozycoffee.wms.location_stock.application.port.`in`.command.DecreaseLocationStockCommand
import com.dozycoffee.wms.location_stock.application.port.`in`.command.IncreaseLocationStockCommand
import com.dozycoffee.wms.location_stock.application.port.`in`.command.TransferLocationStockQualityCommand
import com.dozycoffee.wms.location_stock.application.port.`in`.result.HeldLocationStockResult
import com.dozycoffee.wms.location_stock.application.port.`in`.result.LocationStockResult
import com.dozycoffee.wms.location_stock.application.port.out.LocationStockRepository
import com.dozycoffee.wms.location_stock.domain.enumeration.StockQualityStatus
import com.dozycoffee.wms.location_stock.domain.exception.DuplicateLocationStockException
import com.dozycoffee.wms.location_stock.domain.exception.LocationStockErrorCode
import com.dozycoffee.wms.location_stock.domain.exception.LocationStockNotFoundException
import com.dozycoffee.wms.location_stock.domain.model.LocationStock
import com.dozycoffee.wms.warehouse.application.port.`in`.OccupyLocationUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.ReleaseLocationUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.command.OccupyLocationCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.command.ReleaseLocationCommand
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Location 장부(ADR-0018, ADR-0024). 수량을 바꾸는 증감·소비는 Location 사용량(`usedCapacity`)도 함께 갱신한다 — 사용량은 장부가 소유한다.
 * 품질 전환과 점유는 같은 Location 안의 이동이라 사용량이 바뀌지 않는다.
 */
@Service
class LocationStockService(
    private val locationStockRepository: LocationStockRepository,
    private val occupyLocationUseCase: OccupyLocationUseCase,
    private val releaseLocationUseCase: ReleaseLocationUseCase
) : IncreaseLocationStockUseCase,
    DecreaseLocationStockUseCase,
    TransferLocationStockQualityUseCase,
    HoldLocationStockUseCase,
    ScheduleLocationStockDisposalUseCase,
    GetLocationStockUseCase {

    @Transactional
    override suspend fun increase(command: IncreaseLocationStockCommand): LocationStockResult {
        val saved = addTo(command.locationId, command.productId, command.lotNumber, command.qualityStatus, command.quantity)
        occupyLocationUseCase.occupy(OccupyLocationCommand(command.locationId, command.quantity))
        return LocationStockResult.from(saved)
    }

    @Transactional
    override suspend fun decrease(command: DecreaseLocationStockCommand): LocationStockResult {
        val saved = subtractFrom(command.locationId, command.productId, command.lotNumber, command.qualityStatus, command.quantity)
        releaseLocationUseCase.release(ReleaseLocationCommand(command.locationId, command.quantity))
        return LocationStockResult.from(saved)
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

    @Transactional
    override suspend fun holdForOutbound(
        warehouseId: Long,
        productId: Long,
        lotNumber: String,
        quantity: Int
    ): List<HeldLocationStockResult> {
        val held = mutableListOf<HeldLocationStockResult>()
        var remaining = quantity
        for (row in locationStockRepository.findAvailableForUpdate(warehouseId, productId, lotNumber, StockQualityStatus.NORMAL)) {
            if (remaining <= 0) break
            val amount = minOf(row.availableQuantity, remaining)
            row.hold(amount)
            val saved = locationStockRepository.save(row)
            held.add(HeldLocationStockResult(requireNotNull(saved.locationStockId), saved.locationId, amount))
            remaining -= amount
        }
        return held
    }

    @Transactional
    override suspend fun releaseHold(locationStockId: Long, quantity: Int) {
        val row = findForUpdateOrThrow(locationStockId)
        row.releaseHold(quantity)
        locationStockRepository.save(row)
    }

    @Transactional
    override suspend fun consume(locationStockId: Long, quantity: Int) {
        val row = findForUpdateOrThrow(locationStockId)
        row.consume(quantity)
        locationStockRepository.save(row)
        releaseLocationUseCase.release(ReleaseLocationCommand(row.locationId, quantity))
    }

    @Transactional
    override suspend fun moveToDisposalScheduled(
        warehouseId: Long,
        productId: Long,
        lotNumber: String,
        quantity: Int
    ): List<HeldLocationStockResult> {
        val moved = mutableListOf<HeldLocationStockResult>()
        var remaining = quantity
        for (row in locationStockRepository.findAvailableForUpdate(warehouseId, productId, lotNumber, StockQualityStatus.NORMAL)) {
            if (remaining <= 0) break
            val amount = minOf(row.availableQuantity, remaining)
            row.decrease(amount)
            locationStockRepository.save(row)
            val target = addTo(row.locationId, productId, lotNumber, StockQualityStatus.DISPOSAL_SCHEDULED, amount)
            moved.add(HeldLocationStockResult(requireNotNull(target.locationStockId), target.locationId, amount))
            remaining -= amount
        }
        return moved
    }

    @Transactional(readOnly = true)
    override suspend fun getById(locationStockId: Long): LocationStockResult {
        return LocationStockResult.from(locationStockRepository.findById(locationStockId) ?: throw LocationStockNotFoundException())
    }

    @Transactional(readOnly = true)
    override suspend fun getAllByIds(locationStockIds: Collection<Long>): List<LocationStockResult> {
        return locationStockRepository.findAllByIds(locationStockIds).map { LocationStockResult.from(it) }
    }

    @Transactional(readOnly = true)
    override suspend fun getAllByLocationId(locationId: Long): List<LocationStockResult> {
        return locationStockRepository.findAllByLocationId(locationId).toList().map { LocationStockResult.from(it) }
    }

    @Transactional(readOnly = true)
    override suspend fun existsLot(productId: Long, lotNumber: String): Boolean {
        return locationStockRepository.existsByProductIdAndLotNumber(productId, lotNumber)
    }

    @Transactional(readOnly = true)
    override suspend fun hasChangedSince(locationStockId: Long, since: LocalDateTime): Boolean {
        return locationStockRepository.existsUpdatedAfter(locationStockId, since)
    }

    private suspend fun findForUpdateOrThrow(locationStockId: Long): LocationStock {
        return locationStockRepository.findByIdForUpdate(locationStockId) ?: throw LocationStockNotFoundException()
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
