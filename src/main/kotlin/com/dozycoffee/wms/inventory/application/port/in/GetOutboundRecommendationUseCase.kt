package com.dozycoffee.wms.inventory.application.port.`in`

import com.dozycoffee.wms.inventory.application.port.`in`.result.OutboundRecommendationResult
import kotlinx.coroutines.flow.Flow

interface GetOutboundRecommendationUseCase {
    fun getAll(): Flow<OutboundRecommendationResult>
}
