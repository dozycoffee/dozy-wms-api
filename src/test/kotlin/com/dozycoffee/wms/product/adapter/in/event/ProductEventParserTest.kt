package com.dozycoffee.wms.product.adapter.`in`.event

import com.dozycoffee.wms.product.domain.enumeration.ProductCategory
import com.dozycoffee.wms.product.domain.enumeration.ProductStatus
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tools.jackson.core.JacksonException
import tools.jackson.databind.json.JsonMapper
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId

class ProductEventParserTest {

    private val parser = ProductEventParser(JsonMapper.builder().build(), Clock.system(ZoneId.of("Asia/Seoul")))

    private fun envelope(
        eventType: String = "PRODUCT_REGISTERED",
        eventVersion: String = "1",
        occurredAt: String = "2026-10-13T10:00:00+09:00",
        data: String = DATA
    ): String =
        """{"eventId":"0199a3c2-8e5a-7f30-8c4b-9d1e2f6a0b73","eventType":"$eventType","eventVersion":$eventVersion,"occurredAt":"$occurredAt","source":"inventory","data":$data}"""

    @Nested
    inner class 정상 {

        @Test
        fun `상품 등록 이벤트를 사본 갱신 명령으로 바꾼다`() {
            val command = parser.parse(envelope())

            assertThat(command).isNotNull
            assertThat(command?.productId).isEqualTo(7L)
            assertThat(command?.productCode).isEqualTo("BEAN-001")
            assertThat(command?.productName).isEqualTo("예가체프")
            assertThat(command?.category).isEqualTo(ProductCategory.BEAN)
            assertThat(command?.unit).isEqualTo("KG")
            assertThat(command?.shelfLifeDays).isEqualTo(180)
            assertThat(command?.productStatus).isEqualTo(ProductStatus.ACTIVE)
        }

        @Test
        fun `발생 시각은 Asia Seoul 로컬 시각으로 바꾼다`() {
            val command = parser.parse(envelope(occurredAt = "2026-10-13T01:00:00Z"))

            assertThat(command?.occurredAt).isEqualTo(LocalDateTime.of(2026, 10, 13, 10, 0, 0))
        }

        @Test
        fun `상태 변경 이벤트도 같은 스냅샷으로 읽는다`() {
            val command = parser.parse(envelope(eventType = "PRODUCT_STATUS_CHANGED", data = DATA.replace("ACTIVE", "INACTIVE")))

            assertThat(command?.productStatus).isEqualTo(ProductStatus.INACTIVE)
        }

        @Test
        fun `유통기한 일수가 없거나 null이면 null이다`() {
            val withoutField = DATA.replace(""","shelfLifeDays":180""", "")
            val withNull = DATA.replace("\"shelfLifeDays\":180", "\"shelfLifeDays\":null")

            assertThat(parser.parse(envelope(data = withoutField))?.shelfLifeDays).isNull()
            assertThat(parser.parse(envelope(data = withNull))?.shelfLifeDays).isNull()
        }

        @Test
        fun `알 수 없는 필드는 무시한다`() {
            val command = parser.parse(envelope(data = DATA.replace("{", """{"newField":"x","""))
                .replace(""""source":"inventory",""", """"source":"inventory","extra":1,"""))

            assertThat(command?.productId).isEqualTo(7L)
        }

        @Test
        fun `상품 사본과 무관한 이벤트 유형은 무시한다`() {
            assertThat(parser.parse(envelope(eventType = "PRODUCT_SOMETHING_ELSE", eventVersion = "9"))).isNull()
        }
    }

    @Nested
    inner class 잘못된_메시지 {

        @Test
        fun `JSON이 아니면 예외를 던진다`() {
            assertThatThrownBy { parser.parse("not json") }.isInstanceOf(JacksonException::class.java)
        }

        @Test
        fun `본문이 비어 있으면 예외를 던진다`() {
            assertThatThrownBy { parser.parse(null) }.isInstanceOf(IllegalArgumentException::class.java)
            assertThatThrownBy { parser.parse(" ") }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `지원하지 않는 eventVersion이면 예외를 던진다`() {
            assertThatThrownBy { parser.parse(envelope(eventVersion = "2")) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("eventVersion")
        }

        @Test
        fun `알 수 없는 카테고리나 상태이면 예외를 던진다`() {
            assertThatThrownBy { parser.parse(envelope(data = DATA.replace("BEAN", "FROZEN"))) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("category")
            assertThatThrownBy { parser.parse(envelope(data = DATA.replace("ACTIVE", "SUSPENDED"))) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("productStatus")
        }

        @Test
        fun `필수 필드가 없으면 예외를 던진다`() {
            assertThatThrownBy { parser.parse(envelope(data = DATA.replace(""""productCode":"BEAN-001",""", ""))) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("productCode")
            assertThatThrownBy { parser.parse(envelope(data = "{}")) }.isInstanceOf(IllegalArgumentException::class.java)
            assertThatThrownBy { parser.parse(envelope().replace(""","data":$DATA""", "")) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `상품 ID나 유통기한 일수가 정수가 아니면 예외를 던진다`() {
            assertThatThrownBy { parser.parse(envelope(data = DATA.replace("\"productId\":7", "\"productId\":\"seven\""))) }
                .isInstanceOf(IllegalArgumentException::class.java)
            assertThatThrownBy { parser.parse(envelope(data = DATA.replace("180", "1.5"))) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `발생 시각 형식이 맞지 않으면 예외를 던진다`() {
            assertThatThrownBy { parser.parse(envelope(occurredAt = "yesterday")) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("occurredAt")
        }
    }

    private companion object {
        const val DATA: String =
            """{"productId":7,"productCode":"BEAN-001","productName":"예가체프","category":"BEAN","unit":"KG","shelfLifeDays":180,"productStatus":"ACTIVE"}"""
    }
}
