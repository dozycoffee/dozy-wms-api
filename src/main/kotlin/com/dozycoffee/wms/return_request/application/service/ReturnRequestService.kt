package com.dozycoffee.wms.return_request.application.service

import com.dozycoffee.wms.disposal.application.port.`in`.RegisterDisposalUseCase
import com.dozycoffee.wms.disposal.application.port.`in`.command.RegisterDisposalCommand
import com.dozycoffee.wms.disposal.application.port.`in`.command.RegisterDisposalItemCommand
import com.dozycoffee.wms.disposal.domain.enumeration.DisposalReason
import com.dozycoffee.wms.inventory.application.port.`in`.GetLotUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.MarkInventoryDefectiveUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.RegisterInventoryUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.RegisterLotUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.command.RegisterInventoryCommand
import com.dozycoffee.wms.inventory.application.port.`in`.command.RegisterLotCommand
import com.dozycoffee.wms.inventory.application.port.`in`.result.InventoryResult
import com.dozycoffee.wms.inventory.application.port.`in`.result.LotResult
import com.dozycoffee.wms.product.application.port.`in`.GetProductUseCase
import com.dozycoffee.wms.return_request.application.port.`in`.CompleteReturnRequestUseCase
import com.dozycoffee.wms.return_request.application.port.`in`.GetReturnRequestUseCase
import com.dozycoffee.wms.return_request.application.port.`in`.RegisterReturnRequestUseCase
import com.dozycoffee.wms.return_request.application.port.`in`.StartReturnInspectingUseCase
import com.dozycoffee.wms.return_request.application.port.`in`.command.CompleteReturnRequestCommand
import com.dozycoffee.wms.return_request.application.port.`in`.command.RegisterReturnRequestCommand
import com.dozycoffee.wms.return_request.application.port.`in`.command.ReturnItemLotAssignmentCommand
import com.dozycoffee.wms.return_request.application.port.`in`.result.ReturnRequestResult
import com.dozycoffee.wms.return_request.application.port.out.ReturnItemRepository
import com.dozycoffee.wms.return_request.application.port.out.ReturnRequestRepository
import com.dozycoffee.wms.return_request.domain.enumeration.ReturnInspectionResult
import com.dozycoffee.wms.return_request.domain.enumeration.ReturnRequestStatus
import com.dozycoffee.wms.return_request.domain.exception.InsufficientZoneCapacityException
import com.dozycoffee.wms.return_request.domain.exception.MissingLotAssignmentException
import com.dozycoffee.wms.return_request.domain.exception.NotAllReturnItemsInspectedException
import com.dozycoffee.wms.return_request.domain.exception.ReturnRequestNotFoundException
import com.dozycoffee.wms.return_request.domain.model.ReturnItem
import com.dozycoffee.wms.return_request.domain.model.ReturnRequest
import com.dozycoffee.wms.warehouse.application.port.`in`.GetLocationUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.GetWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.GetZoneUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.OccupyLocationUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.OccupyWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.ReleaseWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.command.OccupyLocationCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.command.OccupyWorkAreaCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.command.ReleaseWorkAreaCommand
import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReturnRequestService(
    private val returnRequestRepository: ReturnRequestRepository,
    private val returnItemRepository: ReturnItemRepository,
    private val getProductUseCase: GetProductUseCase,
    private val getZoneUseCase: GetZoneUseCase,
    private val getLocationUseCase: GetLocationUseCase,
    private val occupyLocationUseCase: OccupyLocationUseCase,
    private val getWorkAreaUseCase: GetWorkAreaUseCase,
    private val occupyWorkAreaUseCase: OccupyWorkAreaUseCase,
    private val releaseWorkAreaUseCase: ReleaseWorkAreaUseCase,
    private val getLotUseCase: GetLotUseCase,
    private val registerLotUseCase: RegisterLotUseCase,
    private val registerInventoryUseCase: RegisterInventoryUseCase,
    private val markInventoryDefectiveUseCase: MarkInventoryDefectiveUseCase,
    private val registerDisposalUseCase: RegisterDisposalUseCase
) : RegisterReturnRequestUseCase,
    StartReturnInspectingUseCase,
    CompleteReturnRequestUseCase,
    GetReturnRequestUseCase {

    @Transactional
    override suspend fun register(command: RegisterReturnRequestCommand): ReturnRequestResult {
        val returnRequest = ReturnRequest.create(command.warehouseId)
        val savedReturnRequest = returnRequestRepository.save(returnRequest)

        command.items.forEach { item ->
            getProductUseCase.getById(item.productId)
            returnItemRepository.save(
                ReturnItem.create(savedReturnRequest.returnRequestId, item.productId, item.expectedQuantity)
            )
        }

        return ReturnRequestResult.from(savedReturnRequest)
    }

    /** 반품 상품이 반품 처리장에 도착해 검수를 시작할 때 신고 수량만큼 반품 처리장을 점유한다 */
    @Transactional
    override suspend fun startInspecting(returnRequestId: Long): ReturnRequestResult {
        val returnRequest = findReturnRequestOrThrow(returnRequestId)
        val totalExpectedQuantity = returnItemRepository.findAllByReturnRequestId(returnRequestId).toList()
            .sumOf { it.expectedQuantity }

        val workArea = getWorkAreaUseCase.getByWarehouseIdAndAreaCode(returnRequest.warehouseId, AreaCode.RETURN).awaitSingle()
        occupyWorkAreaUseCase.occupy(OccupyWorkAreaCommand(workArea.workAreaId, totalExpectedQuantity)).awaitSingle()

        returnRequest.startInspecting()
        return ReturnRequestResult.from(returnRequestRepository.save(returnRequest))
    }

    /**
     * 모든 반품 상품의 검수가 끝나야 완료할 수 있다. 정상/불량 판정 상품 모두 Lot을 확정하고 Zone 내
     * Location에 분산 배치해 Inventory로 등록한다. 불량 판정 상품은 등록 직후 DEFECTIVE로 전환하고
     * Disposal(REQUESTED, RETURN_DEFECT)로 연계한다 — Inbound의 검수 불량 처리와 동일한 패턴이다.
     * 완료 시 점유했던 반품 처리장을 해제한다.
     */
    @Transactional
    override suspend fun complete(command: CompleteReturnRequestCommand): ReturnRequestResult {
        val returnRequest = findReturnRequestOrThrow(command.returnRequestId)
        val items = returnItemRepository.findAllByReturnRequestId(command.returnRequestId).toList()

        if (items.any { it.inspectionResult == ReturnInspectionResult.PENDING }) {
            throw NotAllReturnItemsInspectedException()
        }

        val lotAssignmentByItemId = command.lotAssignments.associateBy { it.returnItemId }
        val defectiveDisposalItems = mutableListOf<RegisterDisposalItemCommand>()
        items.forEach { item ->
            val assignment = lotAssignmentByItemId[item.returnItemId] ?: throw MissingLotAssignmentException()
            val lot = resolveLot(item.productId, assignment)
            val registeredInventories = distributeToLocations(returnRequest.warehouseId, item, lot.lotId)

            if (item.inspectionResult == ReturnInspectionResult.DEFECTIVE) {
                registeredInventories.forEach { inventory ->
                    markInventoryDefectiveUseCase.markDefective(inventory.inventoryId)
                    defectiveDisposalItems.add(
                        RegisterDisposalItemCommand(inventory.inventoryId, inventory.quantity, DisposalReason.RETURN_DEFECT)
                    )
                }
            }
        }

        if (defectiveDisposalItems.isNotEmpty()) {
            registerDisposalUseCase.register(RegisterDisposalCommand(returnRequest.warehouseId, defectiveDisposalItems))
        }

        val totalExpectedQuantity = items.sumOf { it.expectedQuantity }
        val workArea = getWorkAreaUseCase.getByWarehouseIdAndAreaCode(returnRequest.warehouseId, AreaCode.RETURN).awaitSingle()
        releaseWorkAreaUseCase.release(ReleaseWorkAreaCommand(workArea.workAreaId, totalExpectedQuantity)).awaitSingle()

        returnRequest.complete()
        return ReturnRequestResult.from(returnRequestRepository.save(returnRequest))
    }

    @Transactional(readOnly = true)
    override suspend fun getById(returnRequestId: Long): ReturnRequestResult {
        return ReturnRequestResult.from(findReturnRequestOrThrow(returnRequestId))
    }

    @Transactional(readOnly = true)
    override fun getAll(status: ReturnRequestStatus?): Flow<ReturnRequestResult> {
        return returnRequestRepository.findAll(status).map { ReturnRequestResult.from(it) }
    }

    private suspend fun resolveLot(productId: Long, assignment: ReturnItemLotAssignmentCommand): LotResult {
        val existingLot = getLotUseCase.getAllByProduct(productId).toList()
            .find { it.lotNumber == assignment.lotNumber }
        if (existingLot != null) {
            return existingLot
        }
        return registerLotUseCase.register(
            RegisterLotCommand(assignment.lotNumber, productId, assignment.manufactureDate, assignment.expirationDate)
        )
    }

    /**
     * 상품의 지정 Zone(`product.category` 기준) 내에서 잔여 capacity가 큰 Location부터 채우는
     * First-Fit 방식으로 분산 배치한다 — Inbound의 `distributeToLocations`와 동일한 알고리즘이다.
     */
    private suspend fun distributeToLocations(warehouseId: Long, item: ReturnItem, lotId: Long): List<InventoryResult> {
        val product = getProductUseCase.getById(item.productId)
        val zoneCode = ZoneCode.valueOf(product.category.zoneCode)
        val zone = getZoneUseCase.getByWarehouseIdAndZoneCode(warehouseId, zoneCode).awaitSingle()

        var remainingQuantity = requireNotNull(item.actualQuantity)
        val locations = getLocationUseCase.getByZoneId(zone.zoneId).collectList().awaitSingle()
            .sortedByDescending { it.maxCapacity - it.usedCapacity }

        val registeredInventories = mutableListOf<InventoryResult>()
        for (location in locations) {
            if (remainingQuantity <= 0) break
            val availableCapacity = location.maxCapacity - location.usedCapacity
            if (availableCapacity <= 0) continue

            val allocatedQuantity = minOf(availableCapacity, remainingQuantity)
            occupyLocationUseCase.occupy(OccupyLocationCommand(location.locationId, allocatedQuantity)).awaitSingle()
            registeredInventories.add(
                registerInventoryUseCase.register(
                    RegisterInventoryCommand(lotId, location.locationId, allocatedQuantity, requireNotNull(item.returnItemId))
                )
            )
            remainingQuantity -= allocatedQuantity
        }

        if (remainingQuantity > 0) {
            throw InsufficientZoneCapacityException()
        }
        return registeredInventories
    }

    private suspend fun findReturnRequestOrThrow(returnRequestId: Long): ReturnRequest {
        return returnRequestRepository.findById(returnRequestId) ?: throw ReturnRequestNotFoundException()
    }
}
