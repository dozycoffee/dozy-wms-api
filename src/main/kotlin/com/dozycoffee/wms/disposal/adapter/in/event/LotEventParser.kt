package com.dozycoffee.wms.disposal.adapter.`in`.event

import com.dozycoffee.wms.disposal.application.port.`in`.command.RegisterExpiredLotDisposalCommand
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

/**
 * inventory의 Lot 이벤트(공통 envelope)에서 경과(`LOT_EXPIRED`)만 읽어 폐기 신청 명령으로 바꾼다. 알 수 없는 필드는 무시한다.
 * 형식이 맞지 않거나 지원하지 않는 버전이면 [IllegalArgumentException]을 던져 다시 시도하지 않고 DLT로 보내게 한다.
 * 폐기와 무관한 이벤트 유형은 `null`을 돌려주어 무시한다.
 */
@Component
class LotEventParser(
    private val jsonMapper: JsonMapper
) {

    fun parse(value: String?): RegisterExpiredLotDisposalCommand? {
        require(!value.isNullOrBlank()) { "이벤트 본문이 비어 있다" }
        val root: JsonNode = jsonMapper.readTree(value)
        if (text(root, "eventType") != EXPIRED_EVENT_TYPE) return null
        val eventVersion: Int = integer(root, "eventVersion")
        require(eventVersion == SUPPORTED_EVENT_VERSION) { "지원하지 않는 eventVersion이다: $eventVersion" }
        val data: JsonNode = root.get("data")?.takeIf { it.isObject } ?: throw IllegalArgumentException("data가 없다")

        val transfersNode: JsonNode = data.get("transfers")?.takeIf { it.isArray } ?: throw IllegalArgumentException("transfers가 없다")
        val transfers = transfersNode.values().map {
            RegisterExpiredLotDisposalCommand.Transfer(long(it, "warehouseId"), integer(it, "quantity"))
        }.filter { it.quantity > 0 }
        require(transfers.isNotEmpty()) { "transfers가 비어 있다" }

        return RegisterExpiredLotDisposalCommand(
            eventId = text(root, "eventId"),
            productId = long(data, "productId"),
            lotNumber = text(data, "lotNumber"),
            transfers = transfers
        )
    }

    private fun text(node: JsonNode, field: String): String {
        val child: JsonNode = node.get(field) ?: throw IllegalArgumentException("$field 가 없다")
        require(child.isString && child.asString().isNotBlank()) { "$field 는 비어 있지 않은 문자열이어야 한다" }
        return child.asString()
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
        private const val EXPIRED_EVENT_TYPE = "LOT_EXPIRED"
        private const val SUPPORTED_EVENT_VERSION = 1
    }
}
