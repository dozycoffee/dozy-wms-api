package com.dozycoffee.wms.devdata

import com.dozycoffee.wms.support.SystemActorProvider
import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode
import io.r2dbc.spi.Connection
import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.context.annotation.Import
import org.springframework.core.io.FileSystemResource
import org.springframework.r2dbc.connection.init.ScriptUtils
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.awaitOne
import org.springframework.r2dbc.core.awaitRowsUpdated
import org.springframework.r2dbc.core.flow
import kotlinx.coroutines.flow.toList
import reactor.core.publisher.Mono
import java.nio.file.Path

/** `scripts/seed/dev-master-data.sql`이 코드의 용량 스펙과 어긋나지 않고 재실행에 안전한지 실제 MySQL로 검증한다 */
@DataR2dbcTest
@Import(SystemActorProvider::class)
class DevMasterDataSqlTest {

    @Autowired
    private lateinit var connectionFactory: ConnectionFactory

    @Autowired
    private lateinit var databaseClient: DatabaseClient

    @AfterEach
    fun cleanUp() {
        runTest {
            listOf("location", "zone", "work_area", "warehouse").forEach { table ->
                databaseClient.sql("DELETE FROM $table").fetch().awaitRowsUpdated()
            }
        }
    }

    private suspend fun runSeedScript() {
        Mono.usingWhen(
            connectionFactory.create(),
            { connection: Connection -> ScriptUtils.executeSqlScript(connection, FileSystemResource(SCRIPT)) },
            { connection: Connection -> connection.close() }
        ).awaitSingleOrNull()
    }

    private suspend fun count(table: String): Long =
        databaseClient.sql("SELECT COUNT(*) FROM $table")
            .map { row -> requireNotNull(row.get(0, Number::class.java)).toLong() }
            .awaitOne()

    @Test
    fun `창고 구조를 적재한다`() = runTest {
        runSeedScript()

        assertThat(count("warehouse")).isEqualTo(1)
        assertThat(count("work_area")).isEqualTo(AreaCode.entries.size.toLong())
        assertThat(count("zone")).isEqualTo(ZoneCode.entries.size.toLong())
        assertThat(count("location")).isEqualTo(15)
    }

    @Test
    fun `Zone별 Location 최대 용량 합계는 ZoneCode 용량과 같고 전체는 4320이다`() = runTest {
        runSeedScript()

        val capacityByZone: Map<String, Int> = databaseClient.sql(
            """
            SELECT z.zone_code AS code, SUM(l.max_capacity) AS total
            FROM zone z JOIN location l ON l.zone_id = z.zone_id
            GROUP BY z.zone_code
            """.trimIndent()
        ).map { row ->
            requireNotNull(row.get("code", String::class.java)) to requireNotNull(row.get("total", Number::class.java)).toInt()
        }.flow().toList().toMap()

        ZoneCode.entries.forEach { zoneCode ->
            assertThat(capacityByZone[zoneCode.name]).describedAs(zoneCode.name).isEqualTo(zoneCode.capacity.value)
        }
        assertThat(capacityByZone.values.sum()).isEqualTo(4320)
    }

    @Test
    fun `WorkArea와 Zone 용량 합계는 5000이다`() {
        assertThat(AreaCode.entries.sumOf { it.capacity.value } + ZoneCode.entries.sumOf { it.capacity.value }).isEqualTo(5000)
    }

    @Test
    fun `적재 직후 사용량은 모두 0이고 상태는 AVAILABLE이다`() = runTest {
        runSeedScript()

        assertThat(count("location WHERE used_capacity <> 0 OR location_status <> 'AVAILABLE'")).isEqualTo(0)
        assertThat(count("work_area WHERE used_capacity <> 0 OR work_area_status <> 'AVAILABLE'")).isEqualTo(0)
        assertThat(count("zone WHERE zone_status <> 'AVAILABLE'")).isEqualTo(0)
        assertThat(count("warehouse WHERE warehouse_status <> 'AVAILABLE'")).isEqualTo(0)
    }

    @Test
    fun `다시 실행해도 중복으로 만들지 않는다`() = runTest {
        runSeedScript()
        runSeedScript()

        assertThat(count("warehouse")).isEqualTo(1)
        assertThat(count("work_area")).isEqualTo(4)
        assertThat(count("zone")).isEqualTo(6)
        assertThat(count("location")).isEqualTo(15)
    }

    @Test
    fun `창고가 이미 있으면 아무것도 적재하지 않는다`() = runTest {
        databaseClient.sql(
            """
            INSERT INTO warehouse (warehouse_name, address, latitude, longitude, warehouse_status, created_at, created_by, updated_at, updated_by)
            VALUES ('기존 창고', '서울', 37.5, 127.0, 'AVAILABLE', NOW(6), 'test', NOW(6), 'test')
            """.trimIndent()
        ).fetch().awaitRowsUpdated()

        runSeedScript()

        assertThat(count("warehouse")).isEqualTo(1)
        assertThat(count("work_area")).isEqualTo(0)
        assertThat(count("zone")).isEqualTo(0)
        assertThat(count("location")).isEqualTo(0)
    }

    private companion object {
        val SCRIPT: Path = Path.of("scripts", "seed", "dev-master-data.sql")
    }
}
