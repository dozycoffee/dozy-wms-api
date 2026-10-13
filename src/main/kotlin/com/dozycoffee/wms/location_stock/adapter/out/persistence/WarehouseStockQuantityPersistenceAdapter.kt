package com.dozycoffee.wms.location_stock.adapter.out.persistence

import com.dozycoffee.wms.location_stock.application.port.`in`.result.WarehouseStockQuantityResult
import com.dozycoffee.wms.location_stock.application.port.out.WarehouseStockQuantityRepository
import com.dozycoffee.wms.location_stock.domain.enumeration.StockQualityStatus
import kotlinx.coroutines.flow.toList
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.flow
import org.springframework.stereotype.Component

@Component
class WarehouseStockQuantityPersistenceAdapter(
    private val databaseClient: DatabaseClient
) : WarehouseStockQuantityRepository {

    override suspend fun sumByWarehouse(
        warehouseId: Long,
        productId: Long?,
        lotNumber: String?
    ): List<WarehouseStockQuantityResult> {
        val sql = buildString {
            append(
                """
                SELECT s.product_id, s.lot_number, s.quality_status, SUM(s.quantity) AS quantity
                FROM location_stock s
                JOIN location l ON l.location_id = s.location_id
                JOIN zone z ON z.zone_id = l.zone_id
                WHERE z.warehouse_id = :warehouseId
                """.trimIndent()
            )
            if (productId != null) append("\n  AND s.product_id = :productId")
            if (lotNumber != null) append("\n  AND s.lot_number = :lotNumber")
            append(
                """

                GROUP BY s.product_id, s.lot_number, s.quality_status
                HAVING SUM(s.quantity) > 0
                ORDER BY s.product_id, s.lot_number, s.quality_status
                """.trimIndent()
            )
        }
        var spec = databaseClient.sql(sql).bind("warehouseId", warehouseId)
        if (productId != null) spec = spec.bind("productId", productId)
        if (lotNumber != null) spec = spec.bind("lotNumber", lotNumber)
        return spec
            .map { row, _ ->
                WarehouseStockQuantityResult(
                    productId = requireNotNull(row.get("product_id", java.lang.Long::class.java)).toLong(),
                    lotNumber = requireNotNull(row.get("lot_number", String::class.java)),
                    qualityStatus = StockQualityStatus.valueOf(requireNotNull(row.get("quality_status", String::class.java))),
                    quantity = requireNotNull(row.get("quantity", java.lang.Long::class.java)).toInt()
                )
            }.flow()
            .toList()
    }
}
