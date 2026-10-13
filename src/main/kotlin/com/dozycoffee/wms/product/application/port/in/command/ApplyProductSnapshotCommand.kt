package com.dozycoffee.wms.product.application.port.`in`.command

import com.dozycoffee.wms.product.domain.enumeration.ProductCategory
import com.dozycoffee.wms.product.domain.enumeration.ProductStatus
import java.time.LocalDateTime

/** inventory가 보낸 상품 이벤트의 변경 후 스냅샷. [occurredAt]은 이벤트 발생 시각이다 */
data class ApplyProductSnapshotCommand(
    val productId: Long,
    val productCode: String,
    val productName: String,
    val category: ProductCategory,
    val unit: String,
    val shelfLifeDays: Int?,
    val productStatus: ProductStatus,
    val occurredAt: LocalDateTime
)
