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
import com.dozycoffee.wms.disposal.domain.exception.DisposalQuantityMismatchException
import com.dozycoffee.wms.disposal.domain.exception.InventoryNotDisposableException
import com.dozycoffee.wms.disposal.domain.model.Disposal
import com.dozycoffee.wms.disposal.domain.model.DisposalItem
import com.dozycoffee.wms.inventory.application.port.`in`.ConfirmInventoryDisposalUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.GetInventoryUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.result.InventoryResult
import com.dozycoffee.wms.inventory.domain.enumeration.QualityStatus
import com.dozycoffee.wms.warehouse.application.port.`in`.GetWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.OccupyWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.ReleaseLocationUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.ReleaseWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.command.OccupyWorkAreaCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.command.ReleaseLocationCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.command.ReleaseWorkAreaCommand
import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DisposalService(
    private val disposalRepository: DisposalRepository,
    private val disposalItemRepository: DisposalItemRepository,
    private val getInventoryUseCase: GetInventoryUseCase,
    private val confirmInventoryDisposalUseCase: ConfirmInventoryDisposalUseCase,
    private val getWorkAreaUseCase: GetWorkAreaUseCase,
    private val occupyWorkAreaUseCase: OccupyWorkAreaUseCase,
    private val releaseWorkAreaUseCase: ReleaseWorkAreaUseCase,
    private val releaseLocationUseCase: ReleaseLocationUseCase
) : RegisterDisposalUseCase,
    ApproveDisposalUseCase,
    CompleteDisposalUseCase,
    GetDisposalUseCase {

    /** 대상 재고가 DEFECTIVE/DISPOSAL_SCHEDULED이고 수량이 일치하는 경우에만 폐기 등록을 허용한다 */
    @Transactional
    override suspend fun register(command: RegisterDisposalCommand): DisposalResult {
        val disposal = Disposal.create(command.warehouseId)
        val savedDisposal = disposalRepository.save(disposal)

        command.items.forEach { item ->
            val inventory = getInventoryUseCase.getById(item.inventoryId)
            validateDisposable(inventory, item.quantity)
            disposalItemRepository.save(
                DisposalItem.create(savedDisposal.disposalId, item.inventoryId, item.quantity, item.reason)
            )
        }

        return DisposalResult.from(savedDisposal)
    }

    /** 폐기 승인 — 대상 재고를 폐기 처리장으로 물리 이동시키고 사용량을 갱신한다 */
    @Transactional
    override suspend fun approve(disposalId: Long): DisposalResult {
        val disposal = findDisposalOrThrow(disposalId)
        val totalQuantity = disposalItemRepository.findAllByDisposalId(disposalId).toList().sumOf { it.quantity }

        val workArea = getWorkAreaUseCase.getByWarehouseIdAndAreaCode(disposal.warehouseId, AreaCode.DISPOSAL).awaitSingle()
        occupyWorkAreaUseCase.occupy(OccupyWorkAreaCommand(workArea.workAreaId, totalQuantity)).awaitSingle()

        disposal.approve()
        return DisposalResult.from(disposalRepository.save(disposal))
    }

    /** 폐기 확정 — 대상 재고를 완전히 제외(soft delete)하고 Location/폐기 처리장 점유를 해제한다 */
    @Transactional
    override suspend fun complete(disposalId: Long): DisposalResult {
        val disposal = findDisposalOrThrow(disposalId)
        val items = disposalItemRepository.findAllByDisposalId(disposalId).toList()

        var totalQuantity = 0
        for (item in items) {
            val inventory = getInventoryUseCase.getById(item.inventoryId)
            releaseLocationUseCase.release(ReleaseLocationCommand(inventory.locationId, item.quantity)).awaitSingle()
            confirmInventoryDisposalUseCase.confirmDisposal(item.inventoryId)
            totalQuantity += item.quantity
        }

        val workArea = getWorkAreaUseCase.getByWarehouseIdAndAreaCode(disposal.warehouseId, AreaCode.DISPOSAL).awaitSingle()
        releaseWorkAreaUseCase.release(ReleaseWorkAreaCommand(workArea.workAreaId, totalQuantity)).awaitSingle()

        disposal.complete()
        return DisposalResult.from(disposalRepository.save(disposal))
    }

    @Transactional(readOnly = true)
    override suspend fun getById(disposalId: Long): DisposalResult {
        return DisposalResult.from(findDisposalOrThrow(disposalId))
    }

    @Transactional(readOnly = true)
    override fun getAll(status: DisposalStatus?): Flow<DisposalResult> {
        return disposalRepository.findAll(status).map { DisposalResult.from(it) }
    }

    private fun validateDisposable(inventory: InventoryResult, quantity: Int) {
        if (inventory.qualityStatus == QualityStatus.NORMAL) {
            throw InventoryNotDisposableException()
        }
        if (quantity != inventory.quantity) {
            throw DisposalQuantityMismatchException()
        }
    }

    private suspend fun findDisposalOrThrow(disposalId: Long): Disposal {
        return disposalRepository.findById(disposalId) ?: throw DisposalNotFoundException()
    }
}
