package com.dozycoffee.wms.disposal.adapter.`in`.event

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import tools.jackson.core.JacksonException
import tools.jackson.databind.json.JsonMapper

class LotEventParserTest {

    private val parser = LotEventParser(JsonMapper.builder().build())

    private fun envelope(eventType: String = "LOT_EXPIRED", eventVersion: String = "1", data: String = DATA): String =
        """{"eventId":"0199a3c2-8e5a-7f30-8c4b-9d1e2f6a0b73","eventType":"$eventType","eventVersion":$eventVersion,"occurredAt":"2026-10-14T01:00:00+09:00","source":"inventory","data":$data}"""

    @Test
    fun `Lot 경과 이벤트를 폐기 신청 명령으로 바꾼다`() {
        val command = parser.parse(envelope())

        assertThat(command).isNotNull
        assertThat(command?.eventId).isEqualTo("0199a3c2-8e5a-7f30-8c4b-9d1e2f6a0b73")
        assertThat(command?.productId).isEqualTo(100L)
        assertThat(command?.lotNumber).isEqualTo("LOT-1")
        assertThat(command?.transfers?.map { it.warehouseId to it.quantity }).containsExactly(1L to 7, 2L to 3)
    }

    @Test
    fun `수량이 0인 전환은 건너뛴다`() {
        val data = """{"lotId":7,"productId":100,"lotNumber":"LOT-1","expirationDate":"2026-10-13","transfers":[{"warehouseId":1,"quantity":0},{"warehouseId":2,"quantity":3}],"confirmedReservationIds":[]}"""

        assertThat(parser.parse(envelope(data = data))?.transfers?.map { it.warehouseId }).containsExactly(2L)
    }

    @Test
    fun `폐기와 무관한 Lot 이벤트 유형은 null을 돌려준다`() {
        assertThat(parser.parse(envelope(eventType = "LOT_REGISTERED"))).isNull()
    }

    @Test
    fun `지원하지 않는 버전이거나 전환이 비어 있으면 예외를 던진다`() {
        assertThatThrownBy { parser.parse(envelope(eventVersion = "2")) }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy {
            parser.parse(envelope(data = """{"productId":100,"lotNumber":"LOT-1","transfers":[]}"""))
        }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy {
            parser.parse(envelope(data = """{"productId":100,"lotNumber":"LOT-1"}"""))
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `JSON이 아니거나 비어 있으면 예외를 던진다`() {
        assertThatThrownBy { parser.parse("not json") }.isInstanceOf(JacksonException::class.java)
        assertThatThrownBy { parser.parse(null) }.isInstanceOf(IllegalArgumentException::class.java)
    }

    companion object {
        private const val DATA =
            """{"lotId":7,"productId":100,"lotNumber":"LOT-1","expirationDate":"2026-10-13","transfers":[{"warehouseId":1,"sourceInventoryId":55,"disposalInventoryId":56,"quantity":7},{"warehouseId":2,"sourceInventoryId":65,"disposalInventoryId":66,"quantity":3}],"confirmedReservationIds":[9]}"""
    }
}
