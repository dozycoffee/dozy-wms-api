package com.dozycoffee.wms.devseed

import com.dozycoffee.wms.inbound.application.port.`in`.CompleteInboundUseCase
import com.dozycoffee.wms.inbound.application.port.`in`.GetInboundItemUseCase
import com.dozycoffee.wms.inbound.application.port.`in`.InspectInboundItemUseCase
import com.dozycoffee.wms.inbound.application.port.`in`.RegisterInboundUseCase
import com.dozycoffee.wms.inbound.application.port.`in`.StartInboundProcessingUseCase
import com.dozycoffee.wms.inbound.application.port.`in`.command.CompleteInboundCommand
import com.dozycoffee.wms.inbound.application.port.`in`.command.InboundReceiptCommand
import com.dozycoffee.wms.inbound.application.port.`in`.command.InspectInboundItemCommand
import com.dozycoffee.wms.inbound.domain.enumeration.DefectReason
import com.dozycoffee.wms.inbound.application.port.`in`.command.RegisterInboundCommand
import com.dozycoffee.wms.inbound.application.port.`in`.command.RegisterInboundItemCommand
import com.dozycoffee.wms.inbound.domain.enumeration.InspectionResult
import com.dozycoffee.wms.inventory.application.port.`in`.HoldInventoryUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.ReleaseAllocationUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.command.HoldInventoryCommand
import com.dozycoffee.wms.inventory.domain.enumeration.AllocationReferenceType
import com.dozycoffee.wms.outbound.application.port.`in`.CompleteOutboundUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.GetOutboundItemUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.RegisterOutboundUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.StartOutboundInspectingUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.StartOutboundPickingUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.command.RegisterOutboundCommand
import com.dozycoffee.wms.outbound.application.port.`in`.command.RegisterOutboundItemCommand
import com.dozycoffee.wms.outbound.application.port.`in`.result.OutboundResult
import com.dozycoffee.wms.outbound.domain.enumeration.OutboundStatus
import com.dozycoffee.wms.return_request.application.port.`in`.CompleteReturnRequestUseCase
import com.dozycoffee.wms.return_request.application.port.`in`.GetReturnItemUseCase
import com.dozycoffee.wms.return_request.application.port.`in`.InspectReturnItemUseCase
import com.dozycoffee.wms.return_request.application.port.`in`.RegisterReturnRequestUseCase
import com.dozycoffee.wms.return_request.application.port.`in`.StartReturnInspectingUseCase
import com.dozycoffee.wms.return_request.application.port.`in`.command.CompleteReturnRequestCommand
import com.dozycoffee.wms.return_request.application.port.`in`.command.InspectReturnItemCommand
import com.dozycoffee.wms.return_request.application.port.`in`.command.RegisterReturnItemCommand
import com.dozycoffee.wms.return_request.application.port.`in`.command.RegisterReturnRequestCommand
import com.dozycoffee.wms.return_request.application.port.`in`.command.ReturnItemLotAssignmentCommand
import com.dozycoffee.wms.return_request.domain.enumeration.ReturnInspectionResult
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Component
import java.time.LocalDate

/** 입고·출고·반품 문서를 실제 UseCase 흐름으로 재생해 단계별 상태와 파생 데이터(재고, 이력, 점유)를 만든다 */
@Component
internal class OperationFlowSeeder(
    private val registerInboundUseCase: RegisterInboundUseCase,
    private val startInboundProcessingUseCase: StartInboundProcessingUseCase,
    private val inspectInboundItemUseCase: InspectInboundItemUseCase,
    private val completeInboundUseCase: CompleteInboundUseCase,
    private val getInboundItemUseCase: GetInboundItemUseCase,
    private val registerOutboundUseCase: RegisterOutboundUseCase,
    private val startOutboundPickingUseCase: StartOutboundPickingUseCase,
    private val startOutboundInspectingUseCase: StartOutboundInspectingUseCase,
    private val completeOutboundUseCase: CompleteOutboundUseCase,
    private val getOutboundItemUseCase: GetOutboundItemUseCase,
    private val registerReturnRequestUseCase: RegisterReturnRequestUseCase,
    private val startReturnInspectingUseCase: StartReturnInspectingUseCase,
    private val inspectReturnItemUseCase: InspectReturnItemUseCase,
    private val completeReturnRequestUseCase: CompleteReturnRequestUseCase,
    private val getReturnItemUseCase: GetReturnItemUseCase,
    private val holdInventoryUseCase: HoldInventoryUseCase,
    private val releaseAllocationUseCase: ReleaseAllocationUseCase
) {

    private data class InboundLine(
        val productCode: String,
        val expectedQuantity: Int,
        val actualQuantity: Int? = null,
        val inspectionResult: InspectionResult = InspectionResult.NORMAL,
        val lotNumber: String? = null,
        val expirationOffsetDays: Long? = null
    )

    private data class ReturnLine(
        val productCode: String,
        val expectedQuantity: Int,
        val actualQuantity: Int? = null,
        val inspectionResult: ReturnInspectionResult = ReturnInspectionResult.PENDING,
        val lotNumber: String? = null,
        val expirationOffsetDays: Long? = null
    )

    suspend fun seedInbounds(context: SeedContext, today: LocalDate) {
        registerInbound(context, today.plusDays(2), listOf(InboundLine("SYR-001", 40), InboundLine("PWD-001", 30)))
        registerInbound(context, today.plusDays(5), listOf(InboundLine("SUP-002", 60), InboundLine("SUP-003", 20)))

        completeInbound(
            context, today.minusDays(6), today,
            listOf(
                InboundLine("SYR-003", 60, 58, InspectionResult.NORMAL, "SYR003-L2", 350),
                InboundLine("SUP-002", 40, 40, InspectionResult.NORMAL, "SUP002-L3", null)
            )
        )
        completeInbound(
            context, today.minusDays(3), today,
            listOf(
                InboundLine("PWD-003", 30, 30, InspectionResult.NORMAL, "PWD003-L2", 260),
                InboundLine("PWD-001", 20, 20, InspectionResult.DEFECTIVE, "PWD001-L2", 250)
            )
        )

        val processing = registerInbound(
            context, today,
            listOf(InboundLine("SYR-002", 50), InboundLine("PWD-002", 40), InboundLine("SUP-001", 30))
        )
        startInboundProcessingUseCase.startProcessing(processing.inboundId)
        val itemIdByProduct: Map<Long, Long> = inboundItemIdByProduct(processing.inboundId)
        inspectInbound(context, today, itemIdByProduct, InboundLine("SYR-002", 50, 50, InspectionResult.NORMAL, "SYR002-L2", 350))
        inspectInbound(context, today, itemIdByProduct, InboundLine("PWD-002", 40, 40, InspectionResult.DEFECTIVE, "PWD002-L2", 250))
    }

    suspend fun seedOutbounds(context: SeedContext, baselineInventoryIds: Map<String, Long>) {
        val requested = registerOutbound(context, listOf("BEAN-001" to 30, "SYR-001" to 20))
        seedReleasedAllocation(context, requested, baselineInventoryIds.getValue("SYR001-L1"))

        advanceOutbound(
            registerOutbound(context, listOf("DRY-003" to 20, "SUP-001" to 40)),
            OutboundStatus.PICKING
        )
        advanceOutbound(
            registerOutbound(context, listOf("BEAN-002" to 50, "SYR-001" to 30)),
            OutboundStatus.INSPECTING
        )
        advanceOutbound(registerOutbound(context, listOf("BEAN-001" to 60)), OutboundStatus.COMPLETED)
        advanceOutbound(registerOutbound(context, listOf("SYR-003" to 30, "PWD-001" to 20)), OutboundStatus.COMPLETED)
    }

    suspend fun seedReturns(context: SeedContext, today: LocalDate) {
        registerReturn(context, listOf(ReturnLine("SUP-001", 20)))

        val inspecting = registerReturn(context, listOf(ReturnLine("SUP-003", 25), ReturnLine("SYR-001", 15)))
        startReturnInspectingUseCase.startInspecting(inspecting.returnRequestId)
        inspectReturn(context, inspecting.returnRequestId, ReturnLine("SUP-003", 25, 25, ReturnInspectionResult.NORMAL))

        val completed = registerReturn(context, listOf(ReturnLine("SUP-002", 30), ReturnLine("PWD-002", 10)))
        startReturnInspectingUseCase.startInspecting(completed.returnRequestId)
        val lines = listOf(
            ReturnLine("SUP-002", 30, 30, ReturnInspectionResult.NORMAL, "SUP002-R1", null),
            ReturnLine("PWD-002", 10, 10, ReturnInspectionResult.DEFECTIVE, "PWD002-R1", 120)
        )
        lines.forEach { inspectReturn(context, completed.returnRequestId, it) }
        val itemIdByProduct: Map<Long, Long> = returnItemIdByProduct(completed.returnRequestId)
        completeReturnRequestUseCase.complete(
            CompleteReturnRequestCommand(
                completed.returnRequestId,
                lines.map { line ->
                    ReturnItemLotAssignmentCommand(
                        itemIdByProduct.getValue(context.productIdByCode.getValue(line.productCode)),
                        requireNotNull(line.lotNumber),
                        today.minusDays(RECEIVED_LOT_AGE_DAYS),
                        line.expirationOffsetDays?.let { today.plusDays(it) }
                    )
                }
            )
        )
    }

    private suspend fun registerInbound(context: SeedContext, arrivalDate: LocalDate, lines: List<InboundLine>) =
        registerInboundUseCase.register(
            RegisterInboundCommand(
                context.warehouseId,
                arrivalDate,
                lines.map { RegisterInboundItemCommand(context.productIdByCode.getValue(it.productCode), it.expectedQuantity) }
            )
        )

    private suspend fun completeInbound(
        context: SeedContext,
        arrivalDate: LocalDate,
        today: LocalDate,
        lines: List<InboundLine>
    ) {
        val inbound = registerInbound(context, arrivalDate, lines)
        startInboundProcessingUseCase.startProcessing(inbound.inboundId)
        val itemIdByProduct: Map<Long, Long> = inboundItemIdByProduct(inbound.inboundId)
        lines.forEach { inspectInbound(context, today, itemIdByProduct, it) }
        completeInboundUseCase.complete(CompleteInboundCommand(inbound.inboundId))
    }

    private suspend fun inspectInbound(
        context: SeedContext,
        today: LocalDate,
        itemIdByProduct: Map<Long, Long>,
        line: InboundLine
    ) {
        inspectInboundItemUseCase.inspect(
            InspectInboundItemCommand(
                itemIdByProduct.getValue(context.productIdByCode.getValue(line.productCode)),
                listOf(
                    InboundReceiptCommand(
                        requireNotNull(line.lotNumber),
                        today.minusDays(RECEIVED_LOT_AGE_DAYS),
                        line.expirationOffsetDays?.let { today.plusDays(it) },
                        requireNotNull(line.actualQuantity),
                        line.inspectionResult,
                        if (line.inspectionResult == InspectionResult.DEFECTIVE) DefectReason.DAMAGED else null
                    )
                )
            )
        )
    }

    private suspend fun inboundItemIdByProduct(inboundId: Long): Map<Long, Long> =
        getInboundItemUseCase.getAllByInbound(inboundId).toList().associate { it.productId to it.inboundItemId }

    private suspend fun registerOutbound(context: SeedContext, items: List<Pair<String, Int>>): OutboundResult =
        registerOutboundUseCase.register(
            RegisterOutboundCommand(
                context.warehouseId,
                items.map { (productCode, quantity) ->
                    RegisterOutboundItemCommand(context.productIdByCode.getValue(productCode), quantity)
                }
            )
        )

    private suspend fun advanceOutbound(outbound: OutboundResult, target: OutboundStatus) {
        startOutboundPickingUseCase.startPicking(outbound.outboundId)
        if (target == OutboundStatus.PICKING) return
        startOutboundInspectingUseCase.startInspecting(outbound.outboundId)
        if (target == OutboundStatus.INSPECTING) return
        completeOutboundUseCase.complete(outbound.outboundId)
    }

    /** 취소된 점유(RELEASED) 사례 — 요청 상태의 출고 상품에 점유를 걸었다가 해제한다 */
    private suspend fun seedReleasedAllocation(context: SeedContext, outbound: OutboundResult, inventoryId: Long) {
        val syrupItemId: Long = getOutboundItemUseCase.getAllByOutbound(outbound.outboundId).toList()
            .first { it.productId == context.productIdByCode.getValue("SYR-001") }
            .outboundItemId
        val allocation = holdInventoryUseCase.hold(
            HoldInventoryCommand(inventoryId, AllocationReferenceType.OUTBOUND, syrupItemId, RELEASED_HOLD_QUANTITY)
        )
        releaseAllocationUseCase.release(allocation.allocationId)
    }

    private suspend fun registerReturn(context: SeedContext, lines: List<ReturnLine>) =
        registerReturnRequestUseCase.register(
            RegisterReturnRequestCommand(
                context.warehouseId,
                lines.map {
                    RegisterReturnItemCommand(context.productIdByCode.getValue(it.productCode), it.expectedQuantity)
                }
            )
        )

    private suspend fun inspectReturn(context: SeedContext, returnRequestId: Long, line: ReturnLine) {
        inspectReturnItemUseCase.inspect(
            InspectReturnItemCommand(
                returnItemIdByProduct(returnRequestId).getValue(context.productIdByCode.getValue(line.productCode)),
                requireNotNull(line.actualQuantity),
                line.inspectionResult
            )
        )
    }

    private suspend fun returnItemIdByProduct(returnRequestId: Long): Map<Long, Long> =
        getReturnItemUseCase.getAllByReturnRequest(returnRequestId).toList().associate { it.productId to it.returnItemId }

    private companion object {
        const val RECEIVED_LOT_AGE_DAYS: Long = 14L
        const val RELEASED_HOLD_QUANTITY: Int = 10
    }
}
