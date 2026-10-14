package com.dozycoffee.wms.outbound.adapter.`in`.event

import com.dozycoffee.wms.outbound.application.port.`in`.command.CreateOutboundFromReservationCommand
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * inventory의 예약 이벤트(공통 envelope)에서 확정(`RESERVATION_CONFIRMED`)만 읽어 출고 생성 명령으로 바꾼다. 알 수 없는 필드는 무시한다.
 * 형식이 맞지 않거나 지원하지 않는 버전이면 [IllegalArgumentException]을 던져 다시 시도하지 않고 DLT로 보내게 한다.
 * 출고와 무관한 이벤트 유형은 `null`을 돌려주어 무시한다.
 */
@Component
class ReservationEventParser(
    private val jsonMapper: JsonMapper
) {

    fun parse(value: String?): CreateOutboundFromReservationCommand? {
        require(!value.isNullOrBlank()) { "이벤트 본문이 비어 있다" }
        val root: JsonNode = jsonMapper.readTree(value)
        if (text(root, "eventType") != CONFIRMED_EVENT_TYPE) return null
        val eventVersion: Int = integer(root, "eventVersion")
        require(eventVersion == SUPPORTED_EVENT_VERSION) { "지원하지 않는 eventVersion이다: $eventVersion" }
        val data: JsonNode = root.get("data")?.takeIf { it.isObject } ?: throw IllegalArgumentException("data가 없다")

        val items: List<CreateOutboundFromReservationCommand.Item> = array(data, "items").map { item ->
            val allocations = array(item, "allocations").map { allocation ->
                CreateOutboundFromReservationCommand.Allocation(
                    inventoryId = long(allocation, "inventoryId"),
                    lotNumber = text(allocation, "lotNumber"),
                    expirationDate = optionalText(allocation, "expirationDate")?.let(::date),
                    quantity = integer(allocation, "quantity")
                )
            }
            require(allocations.isNotEmpty()) { "allocations가 비어 있다" }
            CreateOutboundFromReservationCommand.Item(long(item, "productId"), allocations)
        }
        require(items.isNotEmpty()) { "items가 비어 있다" }

        return CreateOutboundFromReservationCommand(
            warehouseId = long(data, "warehouseId"),
            reservationId = long(data, "reservationId"),
            channel = optionalText(data, "channel"),
            externalOrderId = optionalText(data, "externalOrderId"),
            items = items
        )
    }

    private fun date(value: String): LocalDate = try {
        LocalDate.parse(value)
    } catch (e: DateTimeParseException) {
        throw IllegalArgumentException("날짜 형식이 맞지 않는다: $value", e)
    }

    private fun array(node: JsonNode, field: String): List<JsonNode> {
        val child: JsonNode = node.get(field) ?: throw IllegalArgumentException("$field 가 없다")
        require(child.isArray) { "$field 는 배열이어야 한다" }
        return child.values().toList()
    }

    private fun text(node: JsonNode, field: String): String {
        val child: JsonNode = node.get(field) ?: throw IllegalArgumentException("$field 가 없다")
        require(child.isString && child.asString().isNotBlank()) { "$field 는 비어 있지 않은 문자열이어야 한다" }
        return child.asString()
    }

    private fun optionalText(node: JsonNode, field: String): String? {
        val child: JsonNode = node.get(field)?.takeUnless { it.isNull } ?: return null
        require(child.isString) { "$field 는 문자열이어야 한다" }
        return child.asString().takeIf { it.isNotBlank() }
    }

    private fun integer(node: JsonNode, field: String): Int {
        val child: JsonNode = node.get(field) ?: throw IllegalArgumentException("$field 가 없다")
        require(child.isIntegralNumber && child.canConvertToInt()) { "$field 는 정수여야 한다" }
        return child.asInt()
    }

    private fun long(node: JsonNode, field: String): Long {
        val child: JsonNode = node.get(field) ?: throw IllegalArgumentException("$field 가 없다")
        require(child.isIntegralNumber && child.canConvertToLong()) { "$field 는 정수여야 한다" }
        return child.asLong()
    }

    companion object {
        private const val CONFIRMED_EVENT_TYPE = "RESERVATION_CONFIRMED"
        private const val SUPPORTED_EVENT_VERSION = 1
    }
}
