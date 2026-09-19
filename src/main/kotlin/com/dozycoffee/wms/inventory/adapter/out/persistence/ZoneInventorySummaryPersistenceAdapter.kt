package com.dozycoffee.wms.inventory.adapter.out.persistence

import com.dozycoffee.wms.global.persistence.CommonCodes
import com.dozycoffee.wms.inventory.application.port.`in`.result.ZoneInventorySummaryResult
import com.dozycoffee.wms.inventory.application.port.out.ZoneInventorySummaryRepository
import com.dozycoffee.wms.inventory.domain.enumeration.QualityStatus
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.flow
import org.springframework.stereotype.Component

@Component
class ZoneInventorySummaryPersistenceAdapter(
    private val databaseClient: DatabaseClient
) : ZoneInventorySummaryRepository {

    /**
     * Capacity(Zone←Location 합산)와 품질상태별 수량(Zone←Location←Inventory 합산)을 한 쿼리로 묶으면
     * Location 1건에 걸린 Inventory 행 수만큼 capacity가 중복 합산된다 — 두 집계를 별도 쿼리로 분리해
     * 합치는 이유.
     */
    override fun findAll(warehouseIds: List<Long>?): Flow<ZoneInventorySummaryResult> = flow {
        val capacities = fetchZoneCapacities(warehouseIds)
        val quantities = fetchQuantitiesByZone(warehouseIds)
        capacities.forEach { capacity ->
            emit(
                ZoneInventorySummaryResult(
                    zoneId = capacity.zoneId,
                    zoneCode = capacity.zoneCode,
                    warehouseId = capacity.warehouseId,
                    maxCapacity = capacity.maxCapacity,
                    usedCapacity = capacity.usedCapacity,
                    quantityByQualityStatus = quantities[capacity.zoneId] ?: emptyMap()
                )
            )
        }
    }

    private suspend fun fetchZoneCapacities(warehouseIds: List<Long>?): List<ZoneCapacityRow> {
        val sql = ZONE_CAPACITY_QUERY_TEMPLATE.format(warehouseFilterClause(warehouseIds))
        return bindWarehouseIds(databaseClient.sql(sql), warehouseIds)
            .map { row, _ ->
                ZoneCapacityRow(
                    zoneId = requireNotNull(row.get("zone_id", java.lang.Long::class.java)).toLong(),
                    zoneCode = ZoneCode.valueOf(requireNotNull(row.get("zone_code", String::class.java))),
                    warehouseId = requireNotNull(row.get("warehouse_id", java.lang.Long::class.java)).toLong(),
                    maxCapacity = requireNotNull(row.get("max_capacity", java.lang.Long::class.java)).toInt(),
                    usedCapacity = requireNotNull(row.get("used_capacity", java.lang.Long::class.java)).toInt()
                )
            }
            .flow()
            .toList()
    }

    private suspend fun fetchQuantitiesByZone(warehouseIds: List<Long>?): Map<Long, Map<QualityStatus, Int>> {
        val sql = ZONE_QUANTITY_QUERY_TEMPLATE.format(warehouseFilterClause(warehouseIds))
        val rows = bindWarehouseIds(databaseClient.sql(sql), warehouseIds)
            .map { row, _ ->
                ZoneQuantityRow(
                    zoneId = requireNotNull(row.get("zone_id", java.lang.Long::class.java)).toLong(),
                    qualityStatus = CommonCodes.fromCode(
                        QualityStatus::class.java,
                        requireNotNull(row.get("quality_status", String::class.java))
                    ),
                    quantity = requireNotNull(row.get("quantity", java.lang.Long::class.java)).toInt()
                )
            }
            .flow()
            .toList()
        return rows.groupBy { it.zoneId }.mapValues { (_, zoneRows) -> zoneRows.associate { it.qualityStatus to it.quantity } }
    }

    /**
     * r2dbc-mysql은 컬렉션 파라미터를 IN절로 자동 전개해주지 않는다 — 리스트 크기만큼 named parameter
     * (`:warehouseId0`, `:warehouseId1`, ...)를 직접 생성해 바인딩한다. SQL 텍스트에 동적으로 반영되는
     * 건 placeholder 개수뿐이고 실제 값은 전부 바인딩 파라미터로 전달되므로 SQL Injection 위험은 없다.
     */
    private fun warehouseFilterClause(warehouseIds: List<Long>?): String {
        if (warehouseIds.isNullOrEmpty()) return "1 = 1"
        val placeholders = warehouseIds.indices.joinToString(", ") { ":$WAREHOUSE_ID_PARAM_PREFIX$it" }
        return "z.warehouse_id IN ($placeholders)"
    }

    private fun bindWarehouseIds(
        spec: DatabaseClient.GenericExecuteSpec,
        warehouseIds: List<Long>?
    ): DatabaseClient.GenericExecuteSpec {
        if (warehouseIds.isNullOrEmpty()) return spec
        return warehouseIds.foldIndexed(spec) { index, acc, warehouseId ->
            acc.bind("$WAREHOUSE_ID_PARAM_PREFIX$index", warehouseId)
        }
    }

    private data class ZoneCapacityRow(
        val zoneId: Long,
        val zoneCode: ZoneCode,
        val warehouseId: Long,
        val maxCapacity: Int,
        val usedCapacity: Int
    )
    private data class ZoneQuantityRow(val zoneId: Long, val qualityStatus: QualityStatus, val quantity: Int)

    companion object {
        private const val WAREHOUSE_ID_PARAM_PREFIX = "warehouseId"

        private const val ZONE_CAPACITY_QUERY_TEMPLATE = """
            SELECT z.zone_id AS zone_id, z.zone_code AS zone_code, z.warehouse_id AS warehouse_id,
                   CAST(COALESCE(SUM(l.max_capacity), 0) AS SIGNED) AS max_capacity,
                   CAST(COALESCE(SUM(l.used_capacity), 0) AS SIGNED) AS used_capacity
            FROM zone z
            LEFT JOIN location l ON l.zone_id = z.zone_id
            WHERE %s
            GROUP BY z.zone_id, z.zone_code, z.warehouse_id
            ORDER BY z.zone_code
        """

        private const val ZONE_QUANTITY_QUERY_TEMPLATE = """
            SELECT l.zone_id AS zone_id, i.quality_status AS quality_status,
                   CAST(COALESCE(SUM(i.quantity), 0) AS SIGNED) AS quantity
            FROM inventory i
            JOIN location l ON l.location_id = i.location_id
            JOIN zone z ON z.zone_id = l.zone_id
            WHERE i.deleted_at IS NULL AND %s
            GROUP BY l.zone_id, i.quality_status
        """
    }
}
