package com.dozycoffee.wms.outbound.application.port.`in`

import com.dozycoffee.wms.outbound.application.port.`in`.command.CreateOutboundFromReservationCommand

interface CreateOutboundFromReservationUseCase {
    /** 같은 창고·예약의 출고가 이미 있으면 만들지 않고 false를 돌려준다 */
    suspend fun create(command: CreateOutboundFromReservationCommand): Boolean
}
