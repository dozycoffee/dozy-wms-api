package com.dozycoffee.wms.global.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WmsRoleTest {

    private val expressions: List<String> = listOf(
        WmsAuthorize.READ,
        WmsAuthorize.INBOUND,
        WmsAuthorize.OUTBOUND,
        WmsAuthorize.RETURN,
        WmsAuthorize.DISPOSAL,
        WmsAuthorize.STOCK_AUDIT,
        WmsAuthorize.ADMIN
    )

    @Test
    fun `인가 식이 참조하는 role은 모두 WmsRole에 정의돼 있다`() {
        val referenced: Set<String> = expressions
            .flatMap { Regex("'([a-z_]+)'").findAll(it).map { match -> match.groupValues[1] }.toList() }
            .toSet()

        assertThat(referenced).isSubsetOf(WmsRole.entries.map { it.code })
    }

    @Test
    fun `조회 식은 모든 WmsRole을 허용한다`() {
        val readable: Set<String> = Regex("'([a-z_]+)'").findAll(WmsAuthorize.READ).map { it.groupValues[1] }.toSet()

        assertThat(readable).containsExactlyInAnyOrderElementsOf(WmsRole.entries.map { it.code })
    }
}
