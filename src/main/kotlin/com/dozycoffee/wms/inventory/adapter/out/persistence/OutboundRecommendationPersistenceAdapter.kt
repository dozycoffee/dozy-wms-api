package com.dozycoffee.wms.inventory.adapter.out.persistence

import com.dozycoffee.wms.global.persistence.CommonCodes
import com.dozycoffee.wms.inventory.application.port.`in`.result.OutboundRecommendationResult
import com.dozycoffee.wms.inventory.application.port.out.OutboundRecommendationRepository
import com.dozycoffee.wms.inventory.domain.enumeration.LotStatus
import com.dozycoffee.wms.inventory.domain.enumeration.QualityStatus
import kotlinx.coroutines.flow.Flow
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.flow
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 유통기한 임박(EXPIRING_SOON) Lot 중 아직 가용 재고가 남아 있는 것만 "우선 출고 권고" 대상으로
 * 조회한다. 별도 알림 엔티티를 두지 않고 Lot 상태 + Inventory 가용수량을 조합한 조회 전용 어댑터다 —
 * NORMAL → EXPIRING_SOON 전환 이후 Lot을 갱신하는 다른 경로가 없어 `lot.updated_at`을 "권고 시점"으로
 * 그대로 사용할 수 있다.
 */
@Component
class OutboundRecommendationPersistenceAdapter(
    private val databaseClient: DatabaseClient
) : OutboundRecommendationRepository {

    override fun findAll(): Flow<OutboundRecommendationResult> {
        return databaseClient.sql(RECOMMENDATION_QUERY)
            .bind("lotStatus", CommonCodes.toCode(LOT_STATUS_GROUP, LotStatus.EXPIRING_SOON))
            .bind("qualityStatus", CommonCodes.toCode(QUALITY_STATUS_GROUP, QualityStatus.NORMAL))
            .map { row, _ ->
                OutboundRecommendationResult(
                    lotId = requireNotNull(row.get("lot_id", java.lang.Long::class.java)).toLong(),
                    lotNumber = requireNotNull(row.get("lot_number", String::class.java)),
                    productId = requireNotNull(row.get("product_id", java.lang.Long::class.java)).toLong(),
                    productName = requireNotNull(row.get("product_name", String::class.java)),
                    expirationDate = requireNotNull(row.get("expiration_date", LocalDate::class.java)),
                    availableQuantity = requireNotNull(row.get("available_quantity", java.lang.Long::class.java)).toInt(),
                    recommendedAt = requireNotNull(row.get("updated_at", LocalDateTime::class.java))
                )
            }
            .flow()
    }

    companion object {
        private const val LOT_STATUS_GROUP = "LOT_STATUS"
        private const val QUALITY_STATUS_GROUP = "QUALITY_STATUS"

        private const val RECOMMENDATION_QUERY = """
            SELECT l.lot_id AS lot_id, l.lot_number AS lot_number, l.expiration_date AS expiration_date,
                   l.updated_at AS updated_at, p.product_id AS product_id, p.product_name AS product_name,
                   CAST(SUM(i.quantity - i.allocated_quantity) AS SIGNED) AS available_quantity
            FROM lot l
            JOIN inventory i ON i.lot_id = l.lot_id AND i.deleted_at IS NULL
            JOIN product p ON p.product_id = l.product_id
            WHERE l.lot_status = :lotStatus AND i.quality_status = :qualityStatus
            GROUP BY l.lot_id, l.lot_number, l.expiration_date, l.updated_at, p.product_id, p.product_name
            HAVING SUM(i.quantity - i.allocated_quantity) > 0
            ORDER BY l.expiration_date ASC
        """
    }
}
