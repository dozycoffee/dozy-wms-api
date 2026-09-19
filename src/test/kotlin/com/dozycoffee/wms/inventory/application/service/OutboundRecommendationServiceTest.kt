package com.dozycoffee.wms.inventory.application.service

import com.dozycoffee.wms.inventory.application.port.`in`.result.OutboundRecommendationResult
import com.dozycoffee.wms.inventory.application.port.out.OutboundRecommendationRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class OutboundRecommendationServiceTest {

    @Mock
    private lateinit var outboundRecommendationRepository: OutboundRecommendationRepository

    @InjectMocks
    private lateinit var outboundRecommendationService: OutboundRecommendationService

    @Test
    fun `우선 출고 권고 목록을 리포지토리로부터 그대로 반환한다`() = runTest {
        val recommendation = OutboundRecommendationResult(
            lotId = 1L,
            lotNumber = "LOT-20260101-001",
            productId = 1L,
            productName = "콜롬비아 원두",
            expirationDate = LocalDate.of(2026, 10, 1),
            availableQuantity = 15,
            recommendedAt = LocalDateTime.of(2026, 9, 19, 1, 0)
        )
        whenever(outboundRecommendationRepository.findAll()).thenReturn(flowOf(recommendation))

        val result = outboundRecommendationService.getAll().toList()

        assertThat(result).containsExactly(recommendation)
    }
}
