package com.dozycoffee.wms.global.persistence

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class CommonCodesTest {

    private enum class SampleStatus { NORMAL, IN_PROGRESS, DISPOSAL_SCHEDULED }

    @Test
    fun `단일 단어 enum 값을 코드로 왕복 변환한다`() {
        val code = CommonCodes.toCode("SAMPLE_STATUS", SampleStatus.NORMAL)

        assertThat(code).isEqualTo("SAMPLE_STATUS_NORMAL")
        assertThat(CommonCodes.fromCode(SampleStatus::class.java, code)).isEqualTo(SampleStatus.NORMAL)
    }

    @Test
    fun `enum 값 자체에 언더스코어가 있어도 정확히 왕복 변환한다`() {
        val code = CommonCodes.toCode("SAMPLE_STATUS", SampleStatus.IN_PROGRESS)

        assertThat(code).isEqualTo("SAMPLE_STATUS_IN_PROGRESS")
        assertThat(CommonCodes.fromCode(SampleStatus::class.java, code)).isEqualTo(SampleStatus.IN_PROGRESS)
    }

    @Test
    fun `그룹 코드와 값 모두 언더스코어를 포함해도 가장 길게 일치하는 상수를 찾는다`() {
        val code = CommonCodes.toCode("SAMPLE_STATUS", SampleStatus.DISPOSAL_SCHEDULED)

        assertThat(CommonCodes.fromCode(SampleStatus::class.java, code)).isEqualTo(SampleStatus.DISPOSAL_SCHEDULED)
    }

    @Test
    fun `일치하는 상수가 없으면 예외를 던진다`() {
        assertThatThrownBy { CommonCodes.fromCode(SampleStatus::class.java, "SAMPLE_STATUS_UNKNOWN") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
