package com.dozycoffee.wms.inventory.adapter.out.persistence

import com.dozycoffee.wms.support.SystemActorProvider
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.context.annotation.Import
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.flow

@DataR2dbcTest
@Import(SystemActorProvider::class)
class InventoryIndexMigrationTest {

    @Autowired
    private lateinit var databaseClient: DatabaseClient

    @Test
    fun `출고 FIFO 피킹용 inventory 복합 인덱스가 컬럼 순서대로 존재한다`() = runBlocking<Unit> {
        assertThat(indexColumns("inventory", "idx_inventory_product_id_quality_status_deleted_at"))
            .containsExactly("product_id", "quality_status", "deleted_at")
    }

    @Test
    fun `유통기한 스캔용 lot 인덱스가 존재한다`() = runBlocking<Unit> {
        assertThat(indexColumns("lot", "idx_lot_expiration_date")).containsExactly("expiration_date")
    }

    private suspend fun indexColumns(table: String, index: String): List<String> =
        databaseClient.sql(
            """
            SELECT column_name FROM information_schema.statistics
            WHERE table_schema = DATABASE() AND table_name = '$table' AND index_name = '$index'
            ORDER BY seq_in_index
            """
        ).map { row -> row.get(0, String::class.java) ?: error("column_name 누락") }
            .flow().toList()
}
