package com.dozycoffee.wms.global.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WmsRoleTest {

    private val expressions: List<String> = listOf(
        WmsAuthorize.READ,
        WmsAuthorize.MASTER_READ,
        WmsAuthorize.INBOUND,
        WmsAuthorize.OUTBOUND,
        WmsAuthorize.RETURN,
        WmsAuthorize.DISPOSAL,
        WmsAuthorize.STOCK_AUDIT,
        WmsAuthorize.ADMIN,
        WmsAuthorize.OUTBOUND_OR_SERVICE,
        WmsAuthorize.SERVICE
    )

    private fun rolesIn(expression: String): Set<String> =
        Regex("'([a-z_]+)'").findAll(expression).map { it.groupValues[1] }.toSet()

    @Test
    fun `인가 식이 참조하는 role은 모두 WmsRole에 정의돼 있다`() {
        val referenced: Set<String> = expressions.flatMap { rolesIn(it) }.toSet()

        assertThat(referenced).isSubsetOf(WmsRole.entries.map { it.code })
    }

    @Test
    fun `업무 데이터 조회 식은 창고 관리자와 admin만 허용하고 viewer와 service는 제외한다`() {
        assertThat(rolesIn(WmsAuthorize.READ))
            .containsExactlyInAnyOrder(WmsRole.WAREHOUSE_MANAGER.code, WmsRole.ADMIN.code)
    }

    @Test
    fun `창고 마스터 조회 식은 서비스 간 호출용 role을 제외한 모든 WmsRole을 허용한다`() {
        assertThat(rolesIn(WmsAuthorize.MASTER_READ)).containsExactlyInAnyOrderElementsOf(
            WmsRole.entries.filter { it != WmsRole.SERVICE }.map { it.code }
        )
    }

    @Test
    fun `viewer는 어떤 쓰기 식에도 들어 있지 않다`() {
        val writes: List<String> = listOf(
            WmsAuthorize.INBOUND, WmsAuthorize.OUTBOUND, WmsAuthorize.RETURN, WmsAuthorize.DISPOSAL,
            WmsAuthorize.STOCK_AUDIT, WmsAuthorize.ADMIN, WmsAuthorize.OUTBOUND_OR_SERVICE, WmsAuthorize.SERVICE
        )

        writes.forEach { assertThat(rolesIn(it)).doesNotContain(WmsRole.VIEWER.code) }
    }

    @Test
    fun `서비스 간 호출 식은 service와 admin만 허용한다`() {
        assertThat(rolesIn(WmsAuthorize.SERVICE)).containsExactlyInAnyOrder(WmsRole.SERVICE.code, WmsRole.ADMIN.code)
    }
}
