package com.dozycoffee.wms.disposal.fixture

import com.dozycoffee.wms.disposal.domain.enumeration.DisposalStatus
import com.dozycoffee.wms.disposal.domain.model.Disposal

class DisposalTestBuilder {

    private var disposalId: Long? = null
    private var warehouseId: Long? = 1L
    private var status: DisposalStatus = DisposalStatus.REQUESTED

    companion object {
        fun disposal(): DisposalTestBuilder = DisposalTestBuilder()
    }

    fun disposalId(disposalId: Long?): DisposalTestBuilder {
        this.disposalId = disposalId
        return this
    }

    fun warehouseId(warehouseId: Long?): DisposalTestBuilder {
        this.warehouseId = warehouseId
        return this
    }

    fun status(status: DisposalStatus): DisposalTestBuilder {
        this.status = status
        return this
    }

    fun build(): Disposal {
        val id: Long? = disposalId
        if (id != null) {
            return Disposal.reconstitute(
                disposalId = id,
                warehouseId = requireNotNull(warehouseId) { "warehouseId는 재구성 시 필수입니다." },
                status = status
            )
        }
        return Disposal.create(warehouseId = warehouseId)
    }
}
