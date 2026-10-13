package com.dozycoffee.wms.product.adapter.`in`.event

import com.dozycoffee.wms.product.application.port.`in`.command.ApplyProductSnapshotCommand
import com.dozycoffee.wms.product.domain.enumeration.ProductCategory
import com.dozycoffee.wms.product.domain.enumeration.ProductStatus
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.time.Clock
import java.time.LocalDateTime
import java.time.OffsetDateTime

/**
 * inventory의 상품 이벤트(공통 envelope)를 읽어 사본 갱신 명령으로 바꾼다. 알 수 없는 필드는 무시한다.
 * 형식이 맞지 않거나 지원하지 않는 버전·카테고리·상태이면 [IllegalArgumentException]을 던져 다시 시도하지 않고 DLT로 보내게 한다.
 * 상품 상태와 무관한 알 수 없는 이벤트 유형은 `null`을 돌려주어 무시한다.
 */
@Component
class ProductEventParser(
    private val jsonMapper: JsonMapper,
    private val clock: Clock
) {

    fun parse(value: String?): ApplyProductSnapshotCommand? {
        require(!value.isNullOrBlank()) { "이벤트 본문이 비어 있다" }
        val root: JsonNode = jsonMapper.readTree(value)
        val eventType: String = text(root, "eventType")
        if (eventType !in SNAPSHOT_EVENT_TYPES) return null
        val eventVersion: Int = integer(root, "eventVersion")
        require(eventVersion == SUPPORTED_EVENT_VERSION) { "지원하지 않는 eventVersion이다: $eventVersion" }
        val data: JsonNode = root.get("data")?.takeIf { it.isObject } ?: throw IllegalArgumentException("data가 없다")

        return ApplyProductSnapshotCommand(
            productId = long(data, "productId"),
            productCode = text(data, "productCode"),
            productName = text(data, "productName"),
            category = enumValue<ProductCategory>(text(data, "category"), "category"),
            unit = text(data, "unit"),
            shelfLifeDays = data.get("shelfLifeDays")?.takeUnless { it.isNull }?.let { integer(data, "shelfLifeDays") },
            productStatus = enumValue<ProductStatus>(text(data, "productStatus"), "productStatus"),
            occurredAt = occurredAt(text(root, "occurredAt"))
        )
    }

    private fun occurredAt(value: String): LocalDateTime {
        val parsed: OffsetDateTime = try {
            OffsetDateTime.parse(value)
        } catch (e: java.time.format.DateTimeParseException) {
            throw IllegalArgumentException("occurredAt 형식이 맞지 않는다: $value", e)
        }
        return parsed.atZoneSameInstant(clock.zone).toLocalDateTime()
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

    private inline fun <reified T : Enum<T>> enumValue(value: String, field: String): T =
        enumValues<T>().firstOrNull { it.name == value } ?: throw IllegalArgumentException("알 수 없는 $field 값이다: $value")

    companion object {
        private val SNAPSHOT_EVENT_TYPES: Set<String> = setOf("PRODUCT_REGISTERED", "PRODUCT_STATUS_CHANGED")
        private const val SUPPORTED_EVENT_VERSION = 1
    }
}
