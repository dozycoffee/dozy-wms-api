package com.dozycoffee.wms.disposal.application.port.`in`

import com.dozycoffee.wms.disposal.application.port.`in`.command.RegisterExpiredLotDisposalCommand

interface RegisterExpiredLotDisposalUseCase {
    /**
     * 유통기한이 지난 Lot의 정상 재고를 폐기 예정으로 옮기고 창고별 폐기 신청(`REQUESTED`)을 만든다. 같은 이벤트·창고는 한 번만 처리한다.
     * 새로 만든 폐기 신청 수를 돌려준다.
     */
    suspend fun register(command: RegisterExpiredLotDisposalCommand): Int
}
