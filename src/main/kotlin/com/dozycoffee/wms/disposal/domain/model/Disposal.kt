package com.dozycoffee.wms.disposal.domain.model

import com.dozycoffee.wms.disposal.domain.enumeration.DisposalStatus
import com.dozycoffee.wms.disposal.domain.exception.DisposalErrorCode
import com.dozycoffee.wms.disposal.domain.exception.InvalidDisposalStatusTransitionException
import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.global.error.DomainValidator.requireNonNull

class Disposal private constructor(
    val disposalId: Long?,
    val warehouseId: Long,
    status: DisposalStatus
) : BaseEntity() {

    var status: DisposalStatus = status
        private set

    companion object {

        fun create(warehouseId: Long?): Disposal {
            val validWarehouseId: Long = requireNonNull(warehouseId, DisposalErrorCode.INVALID_WAREHOUSE_ID)
            return Disposal(
                disposalId = null,
                warehouseId = validWarehouseId,
                status = DisposalStatus.REQUESTED
            )
        }

        fun reconstitute(disposalId: Long, warehouseId: Long, status: DisposalStatus): Disposal {
            return Disposal(disposalId, warehouseId, status)
        }
    }

    /** 폐기를 승인해 대상 재고를 폐기 처리장으로 물리 이동시킬 때 호출한다 */
    fun approve() = transitionTo(DisposalStatus.APPROVED)

    /** 폐기를 확정해 대상 재고를 완전히 소멸시킬 때 호출한다 */
    fun complete() = transitionTo(DisposalStatus.COMPLETED)

    private fun transitionTo(target: DisposalStatus) {
        if (!status.canTransitionTo(target)) {
            throw InvalidDisposalStatusTransitionException()
        }
        status = target
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Disposal) return false
        val id: Long? = disposalId
        return id != null && id == other.disposalId
    }

    override fun hashCode(): Int {
        return disposalId?.hashCode() ?: System.identityHashCode(this)
    }
}
