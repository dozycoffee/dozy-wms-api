package com.dozycoffee.wms.outbound.application.service

import com.dozycoffee.wms.inventory_call.application.port.`in`.CallInventoryDirectlyUseCase
import com.dozycoffee.wms.inventory_call.application.port.`in`.result.InventoryDirectCallResult
import com.dozycoffee.wms.outbound.domain.exception.OutboundInventoryRejectedException
import com.dozycoffee.wms.outbound.domain.exception.OutboundInventoryUnavailableException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate

/**
 * 피킹 불가 Lot을 inventory에 동기로 보고해 재할당받는다 (ADR-0023 — inventory의 판단을 기다리는 작업이라 아웃박스를 쓰지 않는다).
 * 부족이 보고된 재고 행은 inventory에서 신규 할당이 보류되므로, 대체 재고가 모자라 거부돼도 보류는 남는다(inventory ADR-0031).
 */
@Component
class OutboundInventoryReallocator(
    private val callInventoryDirectlyUseCase: CallInventoryDirectlyUseCase,
    private val jsonMapper: JsonMapper
) {

    suspend fun reallocate(reservationId: Long, idempotencyKey: String, shortages: List<Shortage>): List<ReallocatedItem> {
        val body = ReallocationRequestBody(shortages.map { ShortageBody(it.inventoryId, it.shortageQuantity) })
        val result = callInventoryDirectlyUseCase.post(
            "/api/v1/reservations/$reservationId/reallocation",
            idempotencyKey,
            jsonMapper.writeValueAsString(body)
        )
        return when (result) {
            is InventoryDirectCallResult.Accepted -> parse(result.responseBody)
            is InventoryDirectCallResult.Rejected -> {
                log.warn("inventory 재할당 거부: reservationId={}, status={}, body={}", reservationId, result.status, result.responseBody)
                throw OutboundInventoryRejectedException()
            }
            is InventoryDirectCallResult.Unavailable -> {
                log.warn("inventory 재할당 호출 불가: reservationId={}, reason={}", reservationId, result.reason)
                throw OutboundInventoryUnavailableException()
            }
        }
    }

    private fun parse(responseBody: String?): List<ReallocatedItem> {
        require(!responseBody.isNullOrBlank()) { "재할당 응답이 비어 있다" }
        val items: JsonNode = jsonMapper.readTree(responseBody).get("items")?.takeIf { it.isArray }
            ?: throw IllegalArgumentException("재할당 응답에 items가 없다")
        return items.values().map { item ->
            ReallocatedItem(
                item.get("productId").asLong(),
                item.get("allocations").values().map { allocation ->
                    ReallocatedAllocation(
                        allocation.get("inventoryId").asLong(),
                        allocation.get("lotNumber").asString(),
                        allocation.get("expirationDate")?.takeUnless { it.isNull }?.let { LocalDate.parse(it.asString()) },
                        allocation.get("quantity").asInt()
                    )
                }
            )
        }
    }

    data class Shortage(val inventoryId: Long, val shortageQuantity: Int)

    data class ReallocatedItem(val productId: Long, val allocations: List<ReallocatedAllocation>)

    data class ReallocatedAllocation(val inventoryId: Long, val lotNumber: String, val expirationDate: LocalDate?, val quantity: Int)

    private data class ReallocationRequestBody(val shortages: List<ShortageBody>)

    private data class ShortageBody(val inventoryId: Long, val shortageQuantity: Int)

    private companion object {
        val log = LoggerFactory.getLogger(OutboundInventoryReallocator::class.java)
    }
}
