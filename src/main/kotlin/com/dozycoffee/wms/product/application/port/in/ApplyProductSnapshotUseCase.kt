package com.dozycoffee.wms.product.application.port.`in`

import com.dozycoffee.wms.product.application.port.`in`.command.ApplyProductSnapshotCommand

interface ApplyProductSnapshotUseCase {
    /** 스냅샷으로 상품 사본을 교체한다. 이미 반영한 이벤트와 같거나 오래된 이벤트면 무시하고 false를 돌려준다 */
    suspend fun apply(command: ApplyProductSnapshotCommand): Boolean
}
