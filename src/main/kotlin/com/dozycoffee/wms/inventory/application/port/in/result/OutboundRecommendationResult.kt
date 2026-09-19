package com.dozycoffee.wms.inventory.application.port.`in`.result

import java.time.LocalDate
import java.time.LocalDateTime

data class OutboundRecommendationResult(
    val lotId: Long,
    val lotNumber: String,
    val productId: Long,
    val productName: String,
    val expirationDate: LocalDate,
    val availableQuantity: Int,
    val recommendedAt: LocalDateTime
)
