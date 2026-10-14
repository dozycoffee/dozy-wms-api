package com.dozycoffee.wms.outbound.application.port.`in`

import com.dozycoffee.wms.outbound.application.port.`in`.result.OutboundResult

interface ApproveOutboundCancelUseCase {
    /** 상품이 원위치된 것을 확인하고 취소를 확정한다. 점유와 출고장 점유를 풀고 inventory 예약 해제를 요청한다 */
    suspend fun approveCancel(outboundId: Long): OutboundResult
}
