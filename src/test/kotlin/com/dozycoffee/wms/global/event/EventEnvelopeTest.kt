package com.dozycoffee.wms.global.event

import com.fasterxml.jackson.annotation.JsonInclude
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.json.JsonMapper
import java.time.OffsetDateTime
import java.time.ZoneOffset

class EventEnvelopeTest {

    private val jsonMapper: JsonMapper = JsonMapper.builder().disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS).build()

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private data class Body(val principalId: String, val warehouseIds: List<Long>, val note: String? = null)

    @Test
    fun `공통 메타는 최상위에, 본문은 data 아래에 직렬화한다`() {
        val envelope = EventEnvelope(
            eventId = "0199a3c2-8e5a-7f30-8c4b-9d1e2f6a0b73",
            eventType = "WAREHOUSE_ACCESS_CHANGED",
            eventVersion = 1,
            occurredAt = OffsetDateTime.of(2026, 10, 12, 10, 0, 0, 0, ZoneOffset.ofHours(9)),
            source = EventEnvelope.SOURCE_WMS,
            data = Body("p-1", listOf(10L, 20L))
        )

        val node = jsonMapper.readTree(jsonMapper.writeValueAsString(envelope))

        assertThat(node.get("eventId").asString()).isEqualTo("0199a3c2-8e5a-7f30-8c4b-9d1e2f6a0b73")
        assertThat(node.get("eventType").asString()).isEqualTo("WAREHOUSE_ACCESS_CHANGED")
        assertThat(node.get("eventVersion").asInt()).isEqualTo(1)
        assertThat(node.get("occurredAt").asString()).isEqualTo("2026-10-12T10:00:00+09:00")
        assertThat(node.get("source").asString()).isEqualTo("wms")
        assertThat(node.get("data").get("principalId").asString()).isEqualTo("p-1")
        val warehouseIds: List<Long> = node.get("data").get("warehouseIds").values().map { element -> element.asLong() }
        assertThat(warehouseIds).containsExactly(10L, 20L)
    }

    @Test
    fun `값이 null인 본문 필드는 보내지 않는다`() {
        val envelope = EventEnvelope("id", "T", 1, OffsetDateTime.now(), "wms", data = Body("p", emptyList(), note = null))

        val data = jsonMapper.readTree(jsonMapper.writeValueAsString(envelope)).get("data")

        assertThat(data.has("note")).isFalse()
    }

    @Test
    fun `traceparent가 있으면 최상위에 싣고 없으면 필드를 보내지 않는다`() {
        val traced = EventEnvelope(
            "id", "T", 1, OffsetDateTime.now(), "wms", "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01", Body("p", emptyList())
        )
        val untraced = EventEnvelope("id", "T", 1, OffsetDateTime.now(), "wms", data = Body("p", emptyList()))

        val withTrace = jsonMapper.readTree(jsonMapper.writeValueAsString(traced))
        val withoutTrace = jsonMapper.readTree(jsonMapper.writeValueAsString(untraced))

        assertThat(withTrace.get("traceparent").asString()).isEqualTo("00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01")
        assertThat(withoutTrace.has("traceparent")).isFalse()
    }

    @Test
    fun `메타 필드는 정확히 여섯 개다`() {
        val envelope = EventEnvelope("id", "T", 1, OffsetDateTime.now(), "wms", data = Body("p", emptyList()))

        val node = jsonMapper.readTree(jsonMapper.writeValueAsString(envelope))

        assertThat(node.propertyNames().asSequence().toList())
            .containsExactlyInAnyOrder("eventId", "eventType", "eventVersion", "occurredAt", "source", "data")
    }
}
