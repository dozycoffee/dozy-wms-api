package com.dozycoffee.wms.inventory.application.port.`in`

import com.dozycoffee.wms.inventory.application.port.`in`.result.LotDetailResult
import com.dozycoffee.wms.inventory.application.port.`in`.result.LotResult
import kotlinx.coroutines.flow.Flow

interface GetLotUseCase {
    suspend fun getById(lotId: Long): LotResult
    fun getAllByProduct(productId: Long): Flow<LotResult>

    /** 상세 조회 — Zone/Location별 재고 분포를 함께 반환. 임박 여부는 LotResult.lotStatus로 이미 노출됨 */
    suspend fun getDetailById(lotId: Long): LotDetailResult
}
