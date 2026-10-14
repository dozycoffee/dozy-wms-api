package com.dozycoffee.wms.outbound.application.port.`in`

import com.dozycoffee.wms.outbound.application.port.`in`.result.OutboundResult

interface RequestOutboundCancelUseCase {
    /**
     * 출고 취소를 요청한다. 피킹 전이면 바로 취소하고, 피킹 이후 출고 완료 전이면 관리자 승인을 기다린다.
     * 이미 취소를 요청했거나 취소된 출고는 현재 상태를 그대로 돌려주고(재요청에 안전), 출고가 완료된 뒤에는 취소할 수 없다.
     */
    suspend fun requestCancel(outboundId: Long): OutboundResult
}
