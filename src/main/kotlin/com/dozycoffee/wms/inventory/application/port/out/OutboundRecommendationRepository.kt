package com.dozycoffee.wms.inventory.application.port.out

import com.dozycoffee.wms.inventory.application.port.`in`.result.OutboundRecommendationResult
import kotlinx.coroutines.flow.Flow

interface OutboundRecommendationRepository {
    fun findAll(): Flow<OutboundRecommendationResult>
}
