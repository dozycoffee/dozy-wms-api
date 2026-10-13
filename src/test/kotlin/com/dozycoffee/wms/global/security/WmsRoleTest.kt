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
        WmsAuthorize.ADMIN,
        WmsAuthorize.SERVICE
    )

    @Test
    fun `인가 식이 참조하는 role은 모두 WmsRole에 정의돼 있다`() {
        val referenced: Set<String> = expressions
            .flatMap { Regex("'([a-z_]+)'").findAll(it).map { match -> match.groupValues[1] }.toList() }
            .toSet()

        assertThat(referenced).isSubsetOf(WmsRole.entries.map { it.code })
    }

    @Test
    fun `조회 식은 서비스 간 호출용 role을 제외한 모든 WmsRole을 허용한다`() {
        val readable: Set<String> = Regex("'([a-z_]+)'").findAll(WmsAuthorize.READ).map { it.groupValues[1] }.toSet()

        assertThat(readable).containsExactlyInAnyOrderElementsOf(
            WmsRole.entries.filter { it != WmsRole.SERVICE }.map { it.code }
        )
    }

    @Test
    fun `서비스 간 호출 식은 service와 warehouse_admin만 허용한다`() {
        val allowed: Set<String> = Regex("'([a-z_]+)'").findAll(WmsAuthorize.SERVICE).map { it.groupValues[1] }.toSet()

        assertThat(allowed).containsExactlyInAnyOrder(WmsRole.SERVICE.code, WmsRole.WAREHOUSE_ADMIN.code)
    }
}
