package com.dozycoffee.wms.devseed

import com.dozycoffee.wms.disposal.application.port.`in`.ApproveDisposalUseCase
import com.dozycoffee.wms.disposal.application.port.`in`.CompleteDisposalUseCase
import com.dozycoffee.wms.disposal.application.port.`in`.RegisterDisposalUseCase
import com.dozycoffee.wms.disposal.application.port.`in`.command.RegisterDisposalCommand
import com.dozycoffee.wms.disposal.application.port.`in`.command.RegisterDisposalItemCommand
import com.dozycoffee.wms.disposal.application.port.`in`.result.DisposalResult
import com.dozycoffee.wms.disposal.domain.enumeration.DisposalReason
import com.dozycoffee.wms.inventory.application.port.`in`.GetInventoryUseCase
import org.springframework.stereotype.Component

/**
 * 입고/반품 불량으로 자동 등록된 폐기(REQUESTED) 외에, 유통기한 경과 폐기(APPROVED)와
 * 기타 사유 폐기(COMPLETED)를 만든다. 유통기한 스캔 이후에 호출해야 한다.
 */
@Component
internal class DisposalSeeder(
    private val registerDisposalUseCase: RegisterDisposalUseCase,
    private val approveDisposalUseCase: ApproveDisposalUseCase,
    private val completeDisposalUseCase: CompleteDisposalUseCase,
    private val getInventoryUseCase: GetInventoryUseCase
) {

    suspend fun seed(context: SeedContext, baselineInventoryIds: Map<String, Long>) {
        val expired: DisposalResult = register(
            context, baselineInventoryIds.getValue(SeedData.EXPIRED_LOT_NUMBER), DisposalReason.EXPIRED
        )
        approveDisposalUseCase.approve(expired.disposalId)

        val other: DisposalResult = register(
            context, baselineInventoryIds.getValue(SeedData.DEFECTIVE_LOT_NUMBER), DisposalReason.OTHER
        )
        approveDisposalUseCase.approve(other.disposalId)
        completeDisposalUseCase.complete(other.disposalId)
    }

    private suspend fun register(context: SeedContext, inventoryId: Long, reason: DisposalReason): DisposalResult {
        val quantity: Int = getInventoryUseCase.getById(inventoryId).quantity
        return registerDisposalUseCase.register(
            RegisterDisposalCommand(context.warehouseId, listOf(RegisterDisposalItemCommand(inventoryId, quantity, reason)))
        )
    }
}
