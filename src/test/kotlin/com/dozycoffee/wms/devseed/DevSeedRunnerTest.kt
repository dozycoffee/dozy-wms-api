package com.dozycoffee.wms.devseed

import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(properties = ["wms.dev-seed.enabled=true"])
@ActiveProfiles("dev")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DevSeedRunnerTest {

    @Autowired
    private lateinit var databaseClient: DatabaseClient

    @Autowired
    private lateinit var devSeedRunner: DevSeedRunner

    @AfterAll
    fun cleanUp() = runBlocking {
        TABLES_IN_DELETE_ORDER.forEach { table ->
            databaseClient.sql("DELETE FROM $table").fetch().rowsUpdated().awaitSingle()
        }
    }

    private fun <T : Any> query(sql: String, mapper: (io.r2dbc.spi.Readable) -> T): List<T> = runBlocking {
        databaseClient.sql(sql).map { row -> mapper(row) }.all().asFlow().toList()
    }

    private fun countByStatus(table: String, column: String = "status"): Map<String, Long> =
        query("SELECT $column AS code, COUNT(*) AS cnt FROM $table GROUP BY $column") { row ->
            requireNotNull(row.get("code", String::class.java)) to requireNotNull(row.get("cnt", Number::class.java)).toLong()
        }.toMap()

    private fun count(sql: String): Long =
        query(sql) { row -> requireNotNull(row.get(0, Number::class.java)).toLong() }.single()

    @Nested
    inner class 정합성 {

        @Test
        fun `Location 사용량은 소속 재고 수량 합계와 일치한다`() {
            val mismatches: List<String> = query(
                """
                SELECT l.location_code AS code
                FROM location l
                LEFT JOIN inventory i ON i.location_id = l.location_id AND i.deleted_at IS NULL
                GROUP BY l.location_id, l.location_code, l.used_capacity
                HAVING l.used_capacity <> COALESCE(SUM(i.quantity), 0)
                """.trimIndent()
            ) { row -> requireNotNull(row.get("code", String::class.java)) }

            assertThat(mismatches).isEmpty()
        }

        @Test
        fun `재고 점유 수량은 HELD Allocation 수량 합계와 일치한다`() {
            val mismatches: List<Long> = query(
                """
                SELECT i.inventory_id AS id
                FROM inventory i
                LEFT JOIN allocation a ON a.inventory_id = i.inventory_id AND a.status = 'ALLOCATION_STATUS_HELD'
                GROUP BY i.inventory_id, i.allocated_quantity
                HAVING i.allocated_quantity <> COALESCE(SUM(a.quantity), 0)
                """.trimIndent()
            ) { row -> requireNotNull(row.get("id", Number::class.java)).toLong() }

            assertThat(mismatches).isEmpty()
        }

        @Test
        fun `작업 구역 사용량은 진행 중인 문서가 점유한 수량과 일치한다`() {
            val used: Map<String, Int> = query("SELECT area_code AS code, used_capacity AS used FROM work_area") { row ->
                requireNotNull(row.get("code", String::class.java)) to requireNotNull(row.get("used", Number::class.java)).toInt()
            }.toMap()

            assertThat(used).containsEntry("WORK_AREA_TYPE_${AreaCode.INBOUND.name}", 120)
                .containsEntry("WORK_AREA_TYPE_${AreaCode.OUTBOUND.name}", 140)
                .containsEntry("WORK_AREA_TYPE_${AreaCode.RETURN.name}", 40)
                .containsEntry("WORK_AREA_TYPE_${AreaCode.DISPOSAL.name}", 40)
        }

        @Test
        fun `Zone별 Location 최대 용량 합계는 ZoneCode 용량과 같고 전체는 4320이다`() {
            val capacityByZone: Map<String, Int> = query(
                """
                SELECT z.zone_code AS code, SUM(l.max_capacity) AS total
                FROM zone z JOIN location l ON l.zone_id = z.zone_id
                GROUP BY z.zone_code
                """.trimIndent()
            ) { row ->
                requireNotNull(row.get("code", String::class.java)) to requireNotNull(row.get("total", Number::class.java)).toInt()
            }.toMap()

            ZoneCode.entries.forEach { zoneCode ->
                assertThat(capacityByZone[zoneCode.name]).isEqualTo(zoneCode.capacity.value)
            }
            assertThat(capacityByZone.values.sum()).isEqualTo(4320)
        }
    }

    @Nested
    inner class 상태_분포 {

        @Test
        fun `입고는 대기 2, 진행 1, 완료 2건이다`() {
            assertThat(countByStatus("inbound")).containsEntry("INBOUND_STATUS_WAITING", 2L)
                .containsEntry("INBOUND_STATUS_PROCESSING", 1L)
                .containsEntry("INBOUND_STATUS_COMPLETED", 2L)
                .doesNotContainKey("INBOUND_STATUS_EXPECTED")
        }

        @Test
        fun `출고는 요청 1, 피킹 1, 검수 1, 완료 3건이다(실사 시나리오용 1건 포함)`() {
            assertThat(countByStatus("outbound")).containsEntry("OUTBOUND_STATUS_REQUESTED", 1L)
                .containsEntry("OUTBOUND_STATUS_PICKING", 1L)
                .containsEntry("OUTBOUND_STATUS_INSPECTING", 1L)
                .containsEntry("OUTBOUND_STATUS_COMPLETED", 3L)
        }

        @Test
        fun `반품은 접수 1, 검수 1, 완료 1건이다`() {
            assertThat(countByStatus("return_request")).containsEntry("RETURN_STATUS_RECEIVED", 1L)
                .containsEntry("RETURN_STATUS_INSPECTING", 1L)
                .containsEntry("RETURN_STATUS_COMPLETED", 1L)
        }

        @Test
        fun `폐기는 요청 2, 승인 1, 완료 1건이며 사유 4종이 모두 존재한다`() {
            assertThat(countByStatus("disposal")).containsEntry("DISPOSAL_STATUS_REQUESTED", 2L)
                .containsEntry("DISPOSAL_STATUS_APPROVED", 1L)
                .containsEntry("DISPOSAL_STATUS_COMPLETED", 1L)
            assertThat(countByStatus("disposal_item", "reason").keys).containsExactlyInAnyOrder(
                "DISPOSAL_REASON_EXPIRED",
                "DISPOSAL_REASON_INSPECTION_DEFECT",
                "DISPOSAL_REASON_RETURN_DEFECT",
                "DISPOSAL_REASON_OTHER"
            )
        }

        @Test
        fun `실사는 네 단계가 하나씩 있고 마감 건에는 승인자가 있다`() {
            assertThat(countByStatus("stock_audit")).containsEntry("STOCK_AUDIT_STATUS_SCHEDULED", 1L)
                .containsEntry("STOCK_AUDIT_STATUS_IN_PROGRESS", 1L)
                .containsEntry("STOCK_AUDIT_STATUS_COMPLETED", 1L)
                .containsEntry("STOCK_AUDIT_STATUS_CLOSED", 1L)
            assertThat(
                count("SELECT COUNT(*) FROM stock_audit WHERE status = 'STOCK_AUDIT_STATUS_CLOSED' AND approved_by IS NOT NULL")
            ).isEqualTo(1L)
            assertThat(count("SELECT COUNT(*) FROM stock_audit_item WHERE has_uncommitted_movement = 1")).isGreaterThanOrEqualTo(1L)
        }
    }

    @Nested
    inner class 재고_케이스 {

        @Test
        fun `Lot은 임박 5건, 경과 2건이다`() {
            assertThat(countByStatus("lot", "lot_status")).containsEntry("LOT_STATUS_EXPIRING_SOON", 5L)
                .containsEntry("LOT_STATUS_EXPIRED", 2L)
        }

        @Test
        fun `경과 재고는 폐기 예정으로 전환되고 점유 중인 경과 재고는 정상 품질로 남는다`() {
            val heldExpired: Long = count(
                """
                SELECT COUNT(*) FROM inventory i JOIN lot l ON l.lot_id = i.lot_id
                WHERE l.lot_status = 'LOT_STATUS_EXPIRED' AND i.quality_status = 'QUALITY_STATUS_NORMAL' AND i.allocated_quantity > 0
                """.trimIndent()
            )
            val scheduled: Long = count(
                "SELECT COUNT(*) FROM inventory WHERE quality_status = 'QUALITY_STATUS_DISPOSAL_SCHEDULED' AND deleted_at IS NULL"
            )

            assertThat(heldExpired).isEqualTo(1L)
            assertThat(scheduled).isEqualTo(1L)
        }

        @Test
        fun `Allocation은 HELD, RELEASED, FULFILLED가 모두 존재한다`() {
            assertThat(countByStatus("allocation", "status").keys).contains(
                "ALLOCATION_STATUS_HELD",
                "ALLOCATION_STATUS_RELEASED",
                "ALLOCATION_STATUS_FULFILLED"
            )
        }

        @Test
        fun `재고 이력은 입고 출고 폐기 조정이 모두 있고 과거로 분산돼 있다`() {
            assertThat(countByStatus("inventory_history", "history_type").keys).contains(
                "INVENTORY_HISTORY_TYPE_INBOUND",
                "INVENTORY_HISTORY_TYPE_OUTBOUND",
                "INVENTORY_HISTORY_TYPE_DISPOSAL",
                "INVENTORY_HISTORY_TYPE_ADJUSTMENT"
            )
            assertThat(count("SELECT COUNT(*) FROM inventory_history WHERE created_at < NOW(6) - INTERVAL 7 DAY"))
                .isGreaterThan(0L)
        }

        @Test
        fun `상품은 20행이며 비활성 1건과 소프트 삭제 1건을 포함한다`() {
            assertThat(count("SELECT COUNT(*) FROM product")).isEqualTo(20L)
            assertThat(count("SELECT COUNT(*) FROM product WHERE product_status = 'PRODUCT_STATUS_INACTIVE'")).isEqualTo(1L)
            assertThat(count("SELECT COUNT(*) FROM product WHERE deleted_at IS NOT NULL")).isEqualTo(1L)
        }
    }

    @Test
    fun `이미 시드된 DB에서는 다시 실행해도 데이터가 늘어나지 않는다`() {
        val before: Long = count("SELECT COUNT(*) FROM inventory")

        devSeedRunner.run(DefaultApplicationArguments())

        assertThat(count("SELECT COUNT(*) FROM warehouse")).isEqualTo(1L)
        assertThat(count("SELECT COUNT(*) FROM inventory")).isEqualTo(before)
    }

    companion object {
        private val TABLES_IN_DELETE_ORDER: List<String> = listOf(
            "stock_audit_item", "stock_audit", "inventory_history", "disposal_item", "disposal",
            "return_item", "return_request", "outbound_item", "outbound", "inbound_item", "inbound",
            "allocation", "inventory", "lot", "product", "location", "work_area", "zone", "warehouse"
        )
    }
}
