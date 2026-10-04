package com.dozycoffee.wms.global.persistence

import com.dozycoffee.wms.disposal.domain.enumeration.DisposalReason
import com.dozycoffee.wms.disposal.domain.enumeration.DisposalStatus
import com.dozycoffee.wms.inbound.domain.enumeration.InboundStatus
import com.dozycoffee.wms.inbound.domain.enumeration.DefectReason
import com.dozycoffee.wms.inbound.domain.enumeration.InspectionResult
import com.dozycoffee.wms.inbound.domain.enumeration.InspectionStatus
import com.dozycoffee.wms.inventory.domain.enumeration.AllocationReferenceType
import com.dozycoffee.wms.inventory.domain.enumeration.AllocationStatus
import com.dozycoffee.wms.inventory.domain.enumeration.InventoryHistoryType
import com.dozycoffee.wms.inventory.domain.enumeration.LotStatus
import com.dozycoffee.wms.inventory.domain.enumeration.QualityStatus
import com.dozycoffee.wms.outbound.domain.enumeration.OutboundStatus
import com.dozycoffee.wms.product.domain.enumeration.ProductCategory
import com.dozycoffee.wms.product.domain.enumeration.ProductStatus
import com.dozycoffee.wms.return_request.domain.enumeration.ReturnInspectionResult
import com.dozycoffee.wms.return_request.domain.enumeration.ReturnRequestStatus
import com.dozycoffee.wms.stock_audit.domain.enumeration.StockAuditStatus
import com.dozycoffee.wms.support.SystemActorProvider
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.context.annotation.Import
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.awaitOneOrNull

@DataR2dbcTest
@Import(SystemActorProvider::class)
class EnumCheckConstraintMigrationTest {

    @Autowired
    private lateinit var databaseClient: DatabaseClient

    @TestFactory
    fun `CHECK 제약의 허용 값이 enum 상수와 일치한다`(): List<DynamicTest> =
        constraints.map { spec ->
            DynamicTest.dynamicTest(spec.constraintName) {
                runBlocking {
                    val expected: Set<String> = spec.enumClass.enumConstants.map { it.name }.toSet()
                    assertThat(allowedValues(spec.constraintName)).isEqualTo(expected)
                }
            }
        }

    @Test
    fun `common_code 테이블이 존재하지 않는다`() = runBlocking<Unit> {
        val count: Long = databaseClient.sql(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'common_code'"
        ).map { row -> row.get(0, Number::class.java)?.toLong() ?: -1L }
            .awaitOneOrNull() ?: -1L
        assertThat(count).isZero()
    }

    private suspend fun allowedValues(constraintName: String): Set<String> {
        val clause: String = databaseClient.sql(
            "SELECT check_clause FROM information_schema.check_constraints WHERE constraint_schema = DATABASE() AND constraint_name = :name"
        ).bind("name", constraintName)
            .map { row -> row.get(0, String::class.java) ?: "" }
            .awaitOneOrNull() ?: error("CHECK 제약이 없다: $constraintName")
        return Regex("[A-Z][A-Z_]*[A-Z]").findAll(clause).map { it.value }.toSet()
    }

    private data class ConstraintSpec(val constraintName: String, val enumClass: Class<out Enum<*>>)

    private companion object {
        val constraints: List<ConstraintSpec> = listOf(
            ConstraintSpec("ck_warehouse_status", AvailabilityStatus::class.java),
            ConstraintSpec("ck_zone_status", AvailabilityStatus::class.java),
            ConstraintSpec("ck_work_area_status", AvailabilityStatus::class.java),
            ConstraintSpec("ck_location_status", AvailabilityStatus::class.java),
            ConstraintSpec("ck_product_category", ProductCategory::class.java),
            ConstraintSpec("ck_product_status", ProductStatus::class.java),
            ConstraintSpec("ck_lot_status", LotStatus::class.java),
            ConstraintSpec("ck_inventory_quality_status", QualityStatus::class.java),
            ConstraintSpec("ck_allocation_reference_type", AllocationReferenceType::class.java),
            ConstraintSpec("ck_allocation_status", AllocationStatus::class.java),
            ConstraintSpec("ck_inbound_status", InboundStatus::class.java),
            ConstraintSpec("ck_inbound_item_inspection_status", InspectionStatus::class.java),
            ConstraintSpec("ck_inbound_receipt_inspection_result", InspectionResult::class.java),
            ConstraintSpec("ck_inbound_receipt_defect_reason", DefectReason::class.java),
            ConstraintSpec("ck_outbound_status", OutboundStatus::class.java),
            ConstraintSpec("ck_return_request_status", ReturnRequestStatus::class.java),
            ConstraintSpec("ck_return_item_inspection_result", ReturnInspectionResult::class.java),
            ConstraintSpec("ck_disposal_status", DisposalStatus::class.java),
            ConstraintSpec("ck_disposal_item_reason", DisposalReason::class.java),
            ConstraintSpec("ck_inventory_history_type", InventoryHistoryType::class.java),
            ConstraintSpec("ck_stock_audit_status", StockAuditStatus::class.java)
        )
    }
}
