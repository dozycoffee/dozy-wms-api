package com.dozycoffee.wms.devseed

import com.dozycoffee.wms.inventory.application.port.`in`.MarkInventoryDefectiveUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.RegisterInventoryUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.RegisterLotUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.ScanExpirationUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.command.RegisterInventoryCommand
import com.dozycoffee.wms.inventory.application.port.`in`.command.RegisterLotCommand
import com.dozycoffee.wms.inventory.application.port.`in`.result.ExpirationScanResult
import com.dozycoffee.wms.warehouse.application.port.`in`.OccupyLocationUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.command.OccupyLocationCommand
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.awaitRowsUpdated
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
internal class InventorySeeder(
    private val registerLotUseCase: RegisterLotUseCase,
    private val registerInventoryUseCase: RegisterInventoryUseCase,
    private val markInventoryDefectiveUseCase: MarkInventoryDefectiveUseCase,
    private val occupyLocationUseCase: OccupyLocationUseCase,
    private val scanExpirationUseCase: ScanExpirationUseCase,
    private val databaseClient: DatabaseClient
) {

    /** 입고 문서 없이 이관된 기초 재고를 등록하고, Lot 번호 → Inventory ID 매핑을 반환한다 */
    suspend fun seedBaseline(context: SeedContext, today: LocalDate): Map<String, Long> {
        val productByCode: Map<String, ProductSpec> = SeedData.PRODUCTS.associateBy { it.productCode }
        return SeedData.BASELINE_STOCK.associate { spec ->
            val product: ProductSpec = productByCode.getValue(spec.productCode)
            val expirationDate: LocalDate? = spec.expirationOffsetDays?.let { today.plusDays(it) }
            val manufactureDate: LocalDate = expirationDate
                ?.let { expiration -> product.shelfLifeDays?.let { expiration.minusDays(it.toLong()) } }
                ?: today.minusDays(DEFAULT_MANUFACTURE_AGE_DAYS)

            val lot = registerLotUseCase.register(
                RegisterLotCommand(
                    spec.lotNumber,
                    context.productIdByCode.getValue(spec.productCode),
                    manufactureDate,
                    expirationDate
                )
            )
            val locationId: Long = context.locationIdByCode.getValue(spec.locationCode)
            occupyLocationUseCase.occupy(OccupyLocationCommand(locationId, spec.quantity))
            val inventory = registerInventoryUseCase.register(
                RegisterInventoryCommand(lot.lotId, locationId, spec.quantity, INITIAL_REFERENCE_ID)
            )
            if (spec.defective) {
                markInventoryDefectiveUseCase.markDefective(inventory.inventoryId)
            }
            spec.lotNumber to inventory.inventoryId
        }
    }

    suspend fun scanExpiration(): ExpirationScanResult = scanExpirationUseCase.scan()

    /** Auditing이 항상 현재 시각을 기록하므로, 기간 필터/입고일 정렬 시연을 위해 과거로 분산시킨다 */
    suspend fun spreadCreatedAt() {
        databaseClient.sql(
            "UPDATE inventory_history SET created_at = DATE_SUB(created_at, INTERVAL ((inventory_history_id * 7) MOD 55 + 1) DAY)"
        ).fetch().awaitRowsUpdated()
        databaseClient.sql(
            "UPDATE inventory SET created_at = DATE_SUB(created_at, INTERVAL ((inventory_id * 5) MOD 40 + 1) DAY)"
        ).fetch().awaitRowsUpdated()
    }

    private companion object {
        const val INITIAL_REFERENCE_ID: Long = 0L
        const val DEFAULT_MANUFACTURE_AGE_DAYS: Long = 30L
    }
}
