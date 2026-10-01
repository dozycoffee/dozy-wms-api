package com.dozycoffee.wms.devseed

import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Component
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import java.time.LocalDate

/**
 * 개발 DB에 시나리오 기반 목 데이터를 적재한다. `dev` 프로파일과 `wms.dev-seed.enabled=true`가 모두 필요하며,
 * 한 트랜잭션으로 실행되어 중간에 실패하면 전체가 롤백된다.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "wms.dev-seed", name = ["enabled"], havingValue = "true")
internal class DevSeedRunner(
    private val databaseClient: DatabaseClient,
    private val transactionalOperator: TransactionalOperator,
    private val masterDataSeeder: MasterDataSeeder,
    private val inventorySeeder: InventorySeeder,
    private val operationFlowSeeder: OperationFlowSeeder,
    private val disposalSeeder: DisposalSeeder,
    private val stockAuditSeeder: StockAuditSeeder
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        runBlocking {
            if (isAlreadySeeded()) {
                log.info("개발용 목 데이터가 이미 존재해 시드를 건너뜁니다. 초기화는 scripts/reset-dev-db.sh를 사용하세요.")
                return@runBlocking
            }
            transactionalOperator.executeAndAwait { seedAll() }
            log.info("개발용 목 데이터 적재를 완료했습니다.")
        }
    }

    private suspend fun seedAll() {
        val today: LocalDate = LocalDate.now()
        val context: SeedContext = masterDataSeeder.seed()
        val baselineInventoryIds: Map<String, Long> = inventorySeeder.seedBaseline(context, today)

        operationFlowSeeder.seedInbounds(context, today)
        operationFlowSeeder.seedOutbounds(context, baselineInventoryIds)
        operationFlowSeeder.seedReturns(context, today)

        val scanResult = inventorySeeder.scanExpiration()
        log.info("유통기한 스캔 결과: {}", scanResult)

        disposalSeeder.seed(context, baselineInventoryIds)
        inventorySeeder.spreadCreatedAt()
        stockAuditSeeder.seed(context, baselineInventoryIds)
    }

    private suspend fun isAlreadySeeded(): Boolean {
        val warehouseCount: Long = databaseClient.sql("SELECT COUNT(*) FROM warehouse")
            .map { row -> row.get(0, Number::class.java)?.toLong() ?: 0L }
            .one()
            .awaitSingle()
        return warehouseCount > 0
    }

    private companion object {
        val log = LoggerFactory.getLogger(DevSeedRunner::class.java)
    }
}
