package com.dozycoffee.wms.location_stock.adapter.out.persistence

import com.dozycoffee.wms.location_stock.application.port.`in`.query.LocationStockQuery
import com.dozycoffee.wms.location_stock.application.port.`in`.result.LocationStockDetailResult
import com.dozycoffee.wms.location_stock.application.port.out.LocationStockQueryRepository
import com.dozycoffee.wms.location_stock.domain.enumeration.StockQualityStatus
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode
import kotlinx.coroutines.flow.Flow
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.flow
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class LocationStockQueryPersistenceAdapter(
    private val databaseClient: DatabaseClient
) : LocationStockQueryRepository {

    override fun findAll(query: LocationStockQuery): Flow<LocationStockDetailResult> {
        val conditions = mutableListOf<String>()
        val bindings = mutableMapOf<String, Any>()

        query.warehouseIds?.takeIf { it.isNotEmpty() }?.let { warehouseIds ->
            warehouseIds.forEachIndexed { index, id -> bindings["$WAREHOUSE_ID_PARAM_PREFIX$index"] = id }
            conditions += "z.warehouse_id IN (${warehouseIds.indices.joinToString(", ") { ":$WAREHOUSE_ID_PARAM_PREFIX$it" }})"
        }
        query.zoneId?.let { bindings["zoneId"] = it; conditions += "l.zone_id = :zoneId" }
        query.locationId?.let { bindings["locationId"] = it; conditions += "ls.location_id = :locationId" }
        query.productId?.let { bindings["productId"] = it; conditions += "ls.product_id = :productId" }
        query.qualityStatus?.let { bindings["qualityStatus"] = it.name; conditions += "ls.quality_status = :qualityStatus" }
        if (!query.includeEmpty) conditions += "ls.quantity > 0"

        val sql = QUERY_TEMPLATE.format(conditions.joinToString(" AND ").ifEmpty { "1 = 1" })
        val spec = bindings.entries.fold(databaseClient.sql(sql)) { acc, (name, value) -> acc.bind(name, value) }
        return spec.map { row, _ ->
            LocationStockDetailResult(
                locationStockId = requireNotNull(row.get("location_stock_id", java.lang.Long::class.java)).toLong(),
                warehouseId = requireNotNull(row.get("warehouse_id", java.lang.Long::class.java)).toLong(),
                zoneId = requireNotNull(row.get("zone_id", java.lang.Long::class.java)).toLong(),
                zoneCode = ZoneCode.valueOf(requireNotNull(row.get("zone_code", String::class.java))),
                locationId = requireNotNull(row.get("location_id", java.lang.Long::class.java)).toLong(),
                locationCode = requireNotNull(row.get("location_code", String::class.java)),
                productId = requireNotNull(row.get("product_id", java.lang.Long::class.java)).toLong(),
                lotNumber = requireNotNull(row.get("lot_number", String::class.java)),
                qualityStatus = StockQualityStatus.valueOf(requireNotNull(row.get("quality_status", String::class.java))),
                quantity = requireNotNull(row.get("quantity", java.lang.Integer::class.java)).toInt(),
                heldQuantity = requireNotNull(row.get("held_quantity", java.lang.Integer::class.java)).toInt(),
                updatedAt = row.get("updated_at", LocalDateTime::class.java)
            )
        }.flow()
    }

    companion object {
        private const val WAREHOUSE_ID_PARAM_PREFIX = "warehouseId"

        private const val QUERY_TEMPLATE = """
            SELECT ls.location_stock_id AS location_stock_id, z.warehouse_id AS warehouse_id,
                   l.zone_id AS zone_id, z.zone_code AS zone_code,
                   ls.location_id AS location_id, l.location_code AS location_code,
                   ls.product_id AS product_id, ls.lot_number AS lot_number, ls.quality_status AS quality_status,
                   ls.quantity AS quantity, ls.held_quantity AS held_quantity, ls.updated_at AS updated_at
            FROM location_stock ls
            JOIN location l ON l.location_id = ls.location_id
            JOIN zone z ON z.zone_id = l.zone_id
            WHERE %s
            ORDER BY z.warehouse_id, z.zone_code, l.location_code, ls.product_id, ls.lot_number
        """
    }
}
