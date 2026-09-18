package com.dozycoffee.wms.inventory.adapter.out.persistence

import com.dozycoffee.wms.inventory.application.port.`in`.result.LotDistributionResult
import com.dozycoffee.wms.inventory.application.port.out.LotDistributionRepository
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode
import kotlinx.coroutines.flow.Flow
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.flow
import org.springframework.stereotype.Component

@Component
class LotDistributionPersistenceAdapter(
    private val databaseClient: DatabaseClient
) : LotDistributionRepository {

    override fun findAllByLotId(lotId: Long): Flow<LotDistributionResult> {
        return databaseClient.sql(DISTRIBUTION_QUERY)
            .bind("lotId", lotId)
            .map { row, _ ->
                LotDistributionResult(
                    locationId = requireNotNull(row.get("location_id", java.lang.Long::class.java)).toLong(),
                    zoneId = requireNotNull(row.get("zone_id", java.lang.Long::class.java)).toLong(),
                    zoneCode = ZoneCode.valueOf(requireNotNull(row.get("zone_code", String::class.java))),
                    quantity = requireNotNull(row.get("quantity", java.lang.Long::class.java)).toInt()
                )
            }
            .flow()
    }

    companion object {
        private const val DISTRIBUTION_QUERY = """
            SELECT i.location_id AS location_id, l.zone_id AS zone_id, z.zone_code AS zone_code,
                   CAST(SUM(i.quantity) AS SIGNED) AS quantity
            FROM inventory i
            JOIN location l ON l.location_id = i.location_id
            JOIN zone z ON z.zone_id = l.zone_id
            WHERE i.lot_id = :lotId AND i.deleted_at IS NULL
            GROUP BY i.location_id, l.zone_id, z.zone_code
            ORDER BY z.zone_code, i.location_id
        """
    }
}
