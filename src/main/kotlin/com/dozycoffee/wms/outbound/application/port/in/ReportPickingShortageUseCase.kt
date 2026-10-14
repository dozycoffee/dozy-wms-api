package com.dozycoffee.wms.outbound.application.port.`in`

import com.dozycoffee.wms.outbound.application.port.`in`.command.ReportPickingShortageCommand
import com.dozycoffee.wms.outbound.application.port.`in`.result.OutboundAllocationResult

interface ReportPickingShortageUseCase {
    /** 피킹할 수 없는 Lot의 부족 수량을 inventory에 보고해 재할당받고, 재할당 뒤의 전체 할당을 돌려준다 */
    suspend fun report(command: ReportPickingShortageCommand): List<OutboundAllocationResult>
}
