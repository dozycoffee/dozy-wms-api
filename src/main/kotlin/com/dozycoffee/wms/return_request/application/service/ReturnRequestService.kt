package com.dozycoffee.wms.return_request.application.service

import com.dozycoffee.wms.product.application.port.`in`.GetProductUseCase
import com.dozycoffee.wms.return_request.application.port.`in`.CompleteReturnRequestUseCase
import com.dozycoffee.wms.return_request.application.port.`in`.GetReturnRequestUseCase
import com.dozycoffee.wms.return_request.application.port.`in`.RegisterReturnRequestUseCase
import com.dozycoffee.wms.return_request.application.port.`in`.StartReturnInspectingUseCase
import com.dozycoffee.wms.return_request.application.port.`in`.command.RegisterReturnRequestCommand
import com.dozycoffee.wms.return_request.application.port.`in`.result.ReturnRequestResult
import com.dozycoffee.wms.return_request.application.port.out.ReturnItemRepository
import com.dozycoffee.wms.return_request.application.port.out.ReturnRequestRepository
import com.dozycoffee.wms.return_request.domain.enumeration.ReturnInspectionResult
import com.dozycoffee.wms.return_request.domain.enumeration.ReturnRequestStatus
import com.dozycoffee.wms.return_request.domain.exception.NotAllReturnItemsInspectedException
import com.dozycoffee.wms.return_request.domain.exception.ReturnRequestNotFoundException
import com.dozycoffee.wms.return_request.domain.model.ReturnItem
import com.dozycoffee.wms.return_request.domain.model.ReturnRequest
import com.dozycoffee.wms.warehouse.application.port.`in`.GetWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.OccupyWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.ReleaseWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.command.OccupyWorkAreaCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.command.ReleaseWorkAreaCommand
import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 정상 판정된 반품 상품을 재고로 복귀시키거나 불량 판정 상품을 폐기 등록하는 연동은 아직 다루지 않는다
 * — Inbound의 불량 상품 이동이 별도 도메인 부재로 범위 밖이었던 것과 동일하게, Inventory/Disposal
 * 리포지토리 접근이 필요해 후속 PR로 분리한다.
 */
@Service
class ReturnRequestService(
    private val returnRequestRepository: ReturnRequestRepository,
    private val returnItemRepository: ReturnItemRepository,
    private val getProductUseCase: GetProductUseCase,
    private val getWorkAreaUseCase: GetWorkAreaUseCase,
    private val occupyWorkAreaUseCase: OccupyWorkAreaUseCase,
    private val releaseWorkAreaUseCase: ReleaseWorkAreaUseCase
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

    /** 모든 반품 상품의 검수가 끝나야 완료할 수 있으며, 점유했던 반품 처리장을 해제한다 */
    @Transactional
    override suspend fun complete(returnRequestId: Long): ReturnRequestResult {
        val returnRequest = findReturnRequestOrThrow(returnRequestId)
        val items = returnItemRepository.findAllByReturnRequestId(returnRequestId).toList()

        if (items.any { it.inspectionResult == ReturnInspectionResult.PENDING }) {
            throw NotAllReturnItemsInspectedException()
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

    private suspend fun findReturnRequestOrThrow(returnRequestId: Long): ReturnRequest {
        return returnRequestRepository.findById(returnRequestId) ?: throw ReturnRequestNotFoundException()
    }
}
