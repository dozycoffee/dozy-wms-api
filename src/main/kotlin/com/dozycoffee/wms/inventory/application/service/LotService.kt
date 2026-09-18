package com.dozycoffee.wms.inventory.application.service

import com.dozycoffee.wms.inventory.application.port.`in`.GetLotUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.RegisterLotUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.command.RegisterLotCommand
import com.dozycoffee.wms.inventory.application.port.`in`.result.LotDetailResult
import com.dozycoffee.wms.inventory.application.port.`in`.result.LotResult
import com.dozycoffee.wms.inventory.application.port.out.LotDistributionRepository
import com.dozycoffee.wms.inventory.application.port.out.LotRepository
import com.dozycoffee.wms.inventory.domain.exception.DuplicateLotNumberException
import com.dozycoffee.wms.inventory.domain.exception.LotNotFoundException
import com.dozycoffee.wms.inventory.domain.model.Lot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LotService(
    private val lotRepository: LotRepository,
    private val lotDistributionRepository: LotDistributionRepository
) : RegisterLotUseCase, GetLotUseCase {

    @Transactional
    override suspend fun register(command: RegisterLotCommand): LotResult {
        if (lotRepository.existsByProductIdAndLotNumber(command.productId, command.lotNumber)) {
            throw DuplicateLotNumberException()
        }
        val lot = Lot.create(
            command.lotNumber,
            command.productId,
            command.manufactureDate,
            command.expirationDate
        )
        return LotResult.from(lotRepository.save(lot))
    }

    @Transactional(readOnly = true)
    override suspend fun getById(lotId: Long): LotResult {
        return LotResult.from(findLotOrThrow(lotId))
    }

    @Transactional(readOnly = true)
    override fun getAllByProduct(productId: Long): Flow<LotResult> {
        return lotRepository.findAllByProductId(productId).map { LotResult.from(it) }
    }

    @Transactional(readOnly = true)
    override suspend fun getDetailById(lotId: Long): LotDetailResult {
        val lot = findLotOrThrow(lotId)
        val distribution = lotDistributionRepository.findAllByLotId(lotId).toList()
        return LotDetailResult(LotResult.from(lot), distribution)
    }

    private suspend fun findLotOrThrow(lotId: Long): Lot {
        return lotRepository.findById(lotId) ?: throw LotNotFoundException()
    }
}
