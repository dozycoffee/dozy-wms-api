package com.dozycoffee.wms.disposal.application.service

import com.dozycoffee.wms.disposal.application.port.`in`.ApproveDisposalUseCase
import com.dozycoffee.wms.disposal.application.port.`in`.CompleteDisposalUseCase
import com.dozycoffee.wms.disposal.application.port.`in`.GetDisposalUseCase
import com.dozycoffee.wms.disposal.application.port.`in`.RegisterDisposalUseCase
import com.dozycoffee.wms.disposal.application.port.`in`.command.RegisterDisposalCommand
import com.dozycoffee.wms.disposal.application.port.`in`.result.DisposalResult
import com.dozycoffee.wms.disposal.application.port.out.DisposalItemRepository
import com.dozycoffee.wms.disposal.application.port.out.DisposalRepository
import com.dozycoffee.wms.disposal.domain.enumeration.DisposalStatus
import com.dozycoffee.wms.disposal.domain.exception.DisposalNotFoundException
import com.dozycoffee.wms.disposal.domain.exception.DisposalQuantityExceededException
import com.dozycoffee.wms.disposal.domain.exception.DisposalStockWarehouseMismatchException
import com.dozycoffee.wms.disposal.domain.exception.InventoryNotDisposableException
import com.dozycoffee.wms.disposal.domain.model.Disposal
import com.dozycoffee.wms.disposal.domain.model.DisposalItem
import com.dozycoffee.wms.global.security.WarehouseAccessGuard
import com.dozycoffee.wms.location_stock.application.port.`in`.DecreaseLocationStockUseCase
import com.dozycoffee.wms.location_stock.application.port.`in`.GetLocationStockUseCase
import com.dozycoffee.wms.location_stock.application.port.`in`.command.DecreaseLocationStockCommand
import com.dozycoffee.wms.location_stock.application.port.`in`.result.LocationStockResult
import com.dozycoffee.wms.location_stock.domain.enumeration.StockQualityStatus
import com.dozycoffee.wms.warehouse.application.port.`in`.GetLocationUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.GetWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.GetZoneUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.OccupyWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.ReleaseWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.command.OccupyWorkAreaCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.command.ReleaseWorkAreaCommand
import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DisposalService(
    private val warehouseAccessGuard: WarehouseAccessGuard,
    private val disposalRepository: DisposalRepository,
    private val disposalItemRepository: DisposalItemRepository,
    private val getLocationStockUseCase: GetLocationStockUseCase,
    private val decreaseLocationStockUseCase: DecreaseLocationStockUseCase,
    private val getLocationUseCase: GetLocationUseCase,
    private val getZoneUseCase: GetZoneUseCase,
    private val getWorkAreaUseCase: GetWorkAreaUseCase,
    private val occupyWorkAreaUseCase: OccupyWorkAreaUseCase,
    private val releaseWorkAreaUseCase: ReleaseWorkAreaUseCase,
    private val disposalInventoryCallRecorder: DisposalInventoryCallRecorder
) : RegisterDisposalUseCase,
    ApproveDisposalUseCase,
    CompleteDisposalUseCase,
    GetDisposalUseCase {

    /**
     * 대상이 요청한 창고의 정상 품질이 아닌(DEFECTIVE/DISPOSAL_SCHEDULED) 위치 재고이고, 다른 미완료 폐기 신청의 수량을 뺀 수량 안일 때만
     * 등록한다(부분 폐기 허용, ADR-0024).
     */
    @Transactional
    override suspend fun register(command: RegisterDisposalCommand): DisposalResult {
        warehouseAccessGuard.require(command.warehouseId)
        val disposal = Disposal.create(command.warehouseId)
        val savedDisposal = disposalRepository.save(disposal)

        val requestedByStock = mutableMapOf<Long, Int>()
        command.items.forEach { item ->
            val stock = getLocationStockUseCase.getById(item.locationStockId)
            validateDisposable(stock, command.warehouseId)
            val requested = requestedByStock.merge(item.locationStockId, item.quantity, Int::plus) ?: item.quantity
            val pending = disposalItemRepository.sumPendingQuantityByLocationStockId(item.locationStockId)
            if (pending + requested > stock.quantity) {
                throw DisposalQuantityExceededException()
            }
            disposalItemRepository.save(
                DisposalItem.create(savedDisposal.disposalId, item.locationStockId, item.quantity, item.reason)
            )
        }

        return DisposalResult.from(savedDisposal)
    }

    /** 폐기 승인 — 대상 재고를 폐기 처리장으로 물리 이동시키고 사용량을 갱신한다 */
    @Transactional
    override suspend fun approve(disposalId: Long): DisposalResult {
        val disposal = findAccessibleDisposalOrThrow(disposalId)
        val totalQuantity = disposalItemRepository.findAllByDisposalId(disposalId).toList().sumOf { it.quantity }

        val workArea = getWorkAreaUseCase.getByWarehouseIdAndAreaCode(disposal.warehouseId, AreaCode.DISPOSAL)
        occupyWorkAreaUseCase.occupy(OccupyWorkAreaCommand(workArea.workAreaId, totalQuantity))

        disposal.approve()
        return DisposalResult.from(disposalRepository.save(disposal))
    }

    /**
     * 폐기 확정 — 폐기 수량만큼 위치 재고(와 Location 사용량)를 줄이고 폐기 처리장 점유를 해제한다.
     * 폐기 상품마다 inventory 폐기 확정 호출을 같은 트랜잭션에서 아웃박스에 기록한다.
     */
    @Transactional
    override suspend fun complete(disposalId: Long): DisposalResult {
        val disposal = findAccessibleDisposalOrThrow(disposalId)
        val items = disposalItemRepository.findAllByDisposalId(disposalId).toList()

        var totalQuantity = 0
        for (item in items) {
            val stock = getLocationStockUseCase.getById(item.locationStockId)
            decreaseLocationStockUseCase.decrease(
                DecreaseLocationStockCommand(stock.locationId, stock.productId, stock.lotNumber, stock.qualityStatus, item.quantity)
            )
            disposalInventoryCallRecorder.record(disposal.warehouseId, item, stock)
            totalQuantity += item.quantity
        }

        val workArea = getWorkAreaUseCase.getByWarehouseIdAndAreaCode(disposal.warehouseId, AreaCode.DISPOSAL)
        releaseWorkAreaUseCase.release(ReleaseWorkAreaCommand(workArea.workAreaId, totalQuantity))

        disposal.complete()
        return DisposalResult.from(disposalRepository.save(disposal))
    }

    @Transactional(readOnly = true)
    override suspend fun getById(disposalId: Long): DisposalResult {
        return DisposalResult.from(findAccessibleDisposalOrThrow(disposalId))
    }

    @Transactional(readOnly = true)
    override fun getAll(status: DisposalStatus?): Flow<DisposalResult> {
        return warehouseAccessGuard.scoped { warehouseIds -> disposalRepository.findAll(status, warehouseIds) }
            .map { DisposalResult.from(it) }
    }

    private suspend fun validateDisposable(stock: LocationStockResult, warehouseId: Long) {
        if (stock.qualityStatus == StockQualityStatus.NORMAL) {
            throw InventoryNotDisposableException()
        }
        val zoneId = getLocationUseCase.getById(stock.locationId).zoneId
        if (getZoneUseCase.getById(zoneId).warehouseId != warehouseId) {
            throw DisposalStockWarehouseMismatchException()
        }
    }

    private suspend fun findAccessibleDisposalOrThrow(disposalId: Long): Disposal {
        val disposal = disposalRepository.findById(disposalId) ?: throw DisposalNotFoundException()
        warehouseAccessGuard.require(disposal.warehouseId)
        return disposal
    }
}
