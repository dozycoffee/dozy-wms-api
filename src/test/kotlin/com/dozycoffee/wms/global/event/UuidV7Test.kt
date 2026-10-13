package com.dozycoffee.wms.global.event

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.UUID

class UuidV7Test {

    @Test
    fun `버전 7과 RFC 변형 비트를 가진 36자 UUID를 만든다`() {
        val value = UuidV7.generate(1_760_000_000_000L)

        assertThat(value).hasSize(36)
        val uuid = UUID.fromString(value)
        assertThat(uuid.version()).isEqualTo(7)
        assertThat(uuid.variant()).isEqualTo(2)
    }

    @Test
    fun `상위 48비트가 밀리초 시각이라 시각 순으로 정렬된다`() {
        val earlier = UuidV7.generate(1_760_000_000_000L)
        val later = UuidV7.generate(1_760_000_000_001L)

        assertThat(earlier.substring(0, 13)).isLessThan(later.substring(0, 13))
        assertThat(java.lang.Long.parseLong(earlier.substring(0, 8) + earlier.substring(9, 13), 16)).isEqualTo(1_760_000_000_000L)
    }

    @Test
    fun `같은 시각에도 서로 다른 값을 만든다`() {
        assertThat(UuidV7.generate(1L)).isNotEqualTo(UuidV7.generate(1L))
    }

    @Test
    fun `48비트를 넘거나 음수인 시각은 거부한다`() {
        assertThatThrownBy { UuidV7.generate(-1L) }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { UuidV7.generate(1L shl 48) }.isInstanceOf(IllegalArgumentException::class.java)
    }
}
