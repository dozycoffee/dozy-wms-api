package com.dozycoffee.wms.inventory.domain.model

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.global.error.DomainValidator.requireNonNull
import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.inventory.domain.enumeration.LotStatus
import com.dozycoffee.wms.inventory.domain.exception.InvalidLotStatusTransitionException
import com.dozycoffee.wms.inventory.domain.exception.LotErrorCode
import java.time.LocalDate

class Lot private constructor(
    val lotId: Long?,
    val lotNumber: String,
    val productId: Long,
    val manufactureDate: LocalDate?,
    val expirationDate: LocalDate?,
    lotStatus: LotStatus
) : BaseEntity() {

    var lotStatus: LotStatus = lotStatus
        private set

    companion object {

        fun create(
            lotNumber: String?,
            productId: Long?,
            manufactureDate: LocalDate?,
            expirationDate: LocalDate?
        ): Lot {
            val validLotNumber: String = validateLotNumber(lotNumber)
            val validProductId: Long = requireNonNull(productId, LotErrorCode.INVALID_PRODUCT_ID)
            validateExpirationDate(manufactureDate, expirationDate)
            return Lot(
                lotId = null,
                lotNumber = validLotNumber,
                productId = validProductId,
                manufactureDate = manufactureDate,
                expirationDate = expirationDate,
                lotStatus = LotStatus.NORMAL
            )
        }

        fun reconstitute(
            lotId: Long,
            lotNumber: String,
            productId: Long,
            manufactureDate: LocalDate?,
            expirationDate: LocalDate?,
            lotStatus: LotStatus
        ): Lot {
            return Lot(lotId, lotNumber, productId, manufactureDate, expirationDate, lotStatus)
        }

        private fun validateLotNumber(lotNumber: String?): String {
            if (lotNumber.isNullOrBlank()) {
                throw InvalidDomainValueException(LotErrorCode.INVALID_LOT_NUMBER)
            }
            return lotNumber
        }

        private fun validateExpirationDate(manufactureDate: LocalDate?, expirationDate: LocalDate?) {
            if (manufactureDate != null && expirationDate != null && expirationDate.isBefore(manufactureDate)) {
                throw InvalidDomainValueException(LotErrorCode.INVALID_EXPIRATION_DATE)
            }
        }
    }

    /** 배치 스캔에서 유통기한 임박(30일 이내)으로 판정될 때 호출한다 */
    fun markExpiringSoon() {
        transitionTo(LotStatus.EXPIRING_SOON)
    }

    /** 배치 스캔에서 유통기한이 경과했을 때 호출한다 */
    fun markExpired() {
        transitionTo(LotStatus.EXPIRED)
    }

    private fun transitionTo(target: LotStatus) {
        if (!lotStatus.canTransitionTo(target)) {
            throw InvalidLotStatusTransitionException()
        }
        lotStatus = target
    }
}
