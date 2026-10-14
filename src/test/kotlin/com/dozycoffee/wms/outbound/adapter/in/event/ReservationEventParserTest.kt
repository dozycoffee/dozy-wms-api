package com.dozycoffee.wms.outbound.adapter.`in`.event

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import tools.jackson.core.JacksonException
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate

class ReservationEventParserTest {

    private val parser = ReservationEventParser(JsonMapper.builder().build())

    private fun envelope(
        eventType: String = "RESERVATION_CONFIRMED",
        eventVersion: String = "1",
        data: String = DATA
    ): String =
        """{"eventId":"0199a3c2-8e5a-7f30-8c4b-9d1e2f6a0b73","eventType":"$eventType","eventVersion":$eventVersion,"occurredAt":"2026-10-14T10:00:00+09:00","source":"inventory","data":$data}"""

    @Test
    fun `예약 확정 이벤트를 출고 생성 명령으로 바꾼다`() {
        val command = parser.parse(envelope())

        assertThat(command).isNotNull
        assertThat(command?.warehouseId).isEqualTo(10L)
        assertThat(command?.reservationId).isEqualTo(1234L)
        assertThat(command?.channel).isEqualTo("OMS")
        assertThat(command?.externalOrderId).isEqualTo("ORDER-1")
        assertThat(command?.items).hasSize(1)
        val item = command?.items?.first()
        assertThat(item?.productId).isEqualTo(100L)
        assertThat(item?.allocations).hasSize(2)
        assertThat(item?.allocations?.first()?.inventoryId).isEqualTo(55L)
        assertThat(item?.allocations?.first()?.lotNumber).isEqualTo("A")
        assertThat(item?.allocations?.first()?.expirationDate).isEqualTo(LocalDate.of(2027, 1, 1))
        assertThat(item?.allocations?.first()?.quantity).isEqualTo(4)
        assertThat(item?.allocations?.last()?.expirationDate).isNull()
    }

    @Test
    fun `채널과 외부 주문 ID가 없어도 읽는다`() {
        val data = """{"reservationId":1,"warehouseId":10,"items":[{"productId":100,"quantity":1,"allocations":[{"inventoryId":55,"quantity":1,"lotNumber":"A"}]}]}"""

        val command = parser.parse(envelope(data = data))

        assertThat(command?.channel).isNull()
        assertThat(command?.externalOrderId).isNull()
    }

    @Test
    fun `출고와 무관한 예약 이벤트 유형은 null을 돌려준다`() {
        assertThat(parser.parse(envelope(eventType = "RESERVATION_RELEASED"))).isNull()
        assertThat(parser.parse(envelope(eventType = "RESERVATION_CREATED"))).isNull()
    }

    @Test
    fun `지원하지 않는 버전이면 예외를 던진다`() {
        assertThatThrownBy { parser.parse(envelope(eventVersion = "2")) }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `필수 필드가 없거나 할당이 비어 있으면 예외를 던진다`() {
        assertThatThrownBy { parser.parse(envelope(data = """{"warehouseId":10,"items":[]}""")) }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy {
            parser.parse(envelope(data = """{"reservationId":1,"warehouseId":10,"items":[{"productId":100,"allocations":[]}]}"""))
        }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { parser.parse(envelope(data = """{"reservationId":1,"warehouseId":10,"items":[]}""")) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `JSON이 아니거나 비어 있으면 예외를 던진다`() {
        assertThatThrownBy { parser.parse("not json") }.isInstanceOf(JacksonException::class.java)
        assertThatThrownBy { parser.parse("") }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { parser.parse(null) }.isInstanceOf(IllegalArgumentException::class.java)
    }

    companion object {
        private const val DATA =
            """{"reservationId":1234,"warehouseId":10,"channel":"OMS","externalOrderId":"ORDER-1","idempotencyKey":"key-1","items":[{"productId":100,"quantity":6,"allocations":[{"inventoryId":55,"quantity":4,"lotId":7,"lotNumber":"A","expirationDate":"2027-01-01"},{"inventoryId":56,"quantity":2,"lotId":8,"lotNumber":"B"}]}]}"""
    }
}
