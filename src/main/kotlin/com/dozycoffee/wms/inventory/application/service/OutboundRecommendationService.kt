package com.dozycoffee.wms.inventory.application.service

import com.dozycoffee.wms.inventory.application.port.`in`.GetOutboundRecommendationUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.result.OutboundRecommendationResult
import com.dozycoffee.wms.inventory.application.port.out.OutboundRecommendationRepository
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Service

@Service
class OutboundRecommendationService(
    private val outboundRecommendationRepository: OutboundRecommendationRepository
) : GetOutboundRecommendationUseCase {

    override fun getAll(): Flow<OutboundRecommendationResult> {
        return outboundRecommendationRepository.findAll()
    }
}
