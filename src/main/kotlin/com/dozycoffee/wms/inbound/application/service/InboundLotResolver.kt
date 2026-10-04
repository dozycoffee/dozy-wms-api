package com.dozycoffee.wms.inbound.application.service

import com.dozycoffee.wms.inbound.domain.exception.LotExpirationConflictException
import com.dozycoffee.wms.inbound.domain.model.InboundReceipt
import com.dozycoffee.wms.inventory.application.port.`in`.GetLotUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.RegisterLotUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.command.RegisterLotCommand
import com.dozycoffee.wms.inventory.application.port.`in`.result.LotResult
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class InboundLotResolver(
    private val getLotUseCase: GetLotUseCase,
    private val registerLotUseCase: RegisterLotUseCase
) {

    /** 같은 (상품, 로트 번호)가 이미 있는데 유통기한이 다르면 FIFO·만료 스캔 기준이 흔들리므로 거부한다 */
    suspend fun requireNoExpirationConflict(productId: Long, lotNumber: String, expirationDate: LocalDate?) {
        val existing: LotResult? = findExisting(productId, lotNumber)
        if (existing != null && existing.expirationDate != expirationDate) {
            throw LotExpirationConflictException()
        }
    }

    suspend fun resolve(productId: Long, receipt: InboundReceipt): LotResult {
        val existing: LotResult? = findExisting(productId, receipt.lotNumber)
        if (existing != null) {
            if (existing.expirationDate != receipt.expirationDate) {
                throw LotExpirationConflictException()
            }
            return existing
        }
        return registerLotUseCase.register(
            RegisterLotCommand(receipt.lotNumber, productId, receipt.manufactureDate, receipt.expirationDate)
        )
    }

    private suspend fun findExisting(productId: Long, lotNumber: String): LotResult? {
        return getLotUseCase.getAllByProduct(productId).toList().find { it.lotNumber == lotNumber }
    }
}
