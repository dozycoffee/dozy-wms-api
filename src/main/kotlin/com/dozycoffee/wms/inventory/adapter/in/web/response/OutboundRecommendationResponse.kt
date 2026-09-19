package com.dozycoffee.wms.inventory.adapter.`in`.web.response

import com.dozycoffee.wms.inventory.application.port.`in`.result.OutboundRecommendationResult
import java.time.LocalDate
import java.time.LocalDateTime

data class OutboundRecommendationResponse(
    val lotId: Long,
    val lotNumber: String,
    val productId: Long,
    val productName: String,
    val expirationDate: LocalDate,
    val availableQuantity: Int,
    val recommendedAt: LocalDateTime
) {
    companion object {
        fun from(result: OutboundRecommendationResult): OutboundRecommendationResponse {
            return OutboundRecommendationResponse(
                result.lotId,
                result.lotNumber,
                result.productId,
                result.productName,
                result.expirationDate,
                result.availableQuantity,
                result.recommendedAt
            )
        }
    }
}
