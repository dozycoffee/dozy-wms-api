package com.dozycoffee.wms.stock_audit.domain.model

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.global.error.DomainValidator.requireNonNull
import com.dozycoffee.wms.stock_audit.domain.enumeration.StockAuditStatus
import com.dozycoffee.wms.stock_audit.domain.exception.InvalidStockAuditStatusTransitionException
import com.dozycoffee.wms.stock_audit.domain.exception.StockAuditApprovalRequiredException
import com.dozycoffee.wms.stock_audit.domain.exception.StockAuditErrorCode

class StockAudit private constructor(
    val stockAuditId: Long?,
    val warehouseId: Long,
    val zoneId: Long,
    status: StockAuditStatus,
    assignee: String?,
    approvedBy: String?
) : BaseEntity() {

    var status: StockAuditStatus = status
        private set

    var assignee: String? = assignee
        private set

    var approvedBy: String? = approvedBy
        private set

    companion object {

        fun create(warehouseId: Long?, zoneId: Long?): StockAudit {
            val validWarehouseId: Long = requireNonNull(warehouseId, StockAuditErrorCode.INVALID_WAREHOUSE_ID)
            val validZoneId: Long = requireNonNull(zoneId, StockAuditErrorCode.INVALID_ZONE_ID)
            return StockAudit(
                stockAuditId = null,
                warehouseId = validWarehouseId,
                zoneId = validZoneId,
                status = StockAuditStatus.SCHEDULED,
                assignee = null,
                approvedBy = null
            )
        }

        fun reconstitute(
            stockAuditId: Long,
            warehouseId: Long,
            zoneId: Long,
            status: StockAuditStatus,
            assignee: String?,
            approvedBy: String?
        ): StockAudit {
            return StockAudit(stockAuditId, warehouseId, zoneId, status, assignee, approvedBy)
        }
    }

    /** 담당자 배정 시점에 실사가 시작된 것으로 간주해 IN_PROGRESS로 전환한다(ADR-0009) */
    fun assign(assignee: String?) {
        val validAssignee: String =
            requireNonNull(assignee?.takeIf { it.isNotBlank() }, StockAuditErrorCode.INVALID_ASSIGNEE)
        transitionTo(StockAuditStatus.IN_PROGRESS)
        this.assignee = validAssignee
    }

    fun complete() = transitionTo(StockAuditStatus.COMPLETED)

    /**
     * 조정 확정 — requiresApproval(항목별 최종 조정량이 임계치를 초과하는지)은 서비스 계층이 계산해
     * 전달한다. 엔티티는 "초과 시 승인자 없이는 마감할 수 없다"는 불변식만 지킨다(ADR-0009).
     */
    fun close(requiresApproval: Boolean, approvedBy: String?) {
        if (requiresApproval && approvedBy.isNullOrBlank()) {
            throw StockAuditApprovalRequiredException()
        }
        transitionTo(StockAuditStatus.CLOSED)
        this.approvedBy = approvedBy
    }

    private fun transitionTo(target: StockAuditStatus) {
        if (!status.canTransitionTo(target)) {
            throw InvalidStockAuditStatusTransitionException()
        }
        status = target
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StockAudit) return false
        val id: Long? = stockAuditId
        return id != null && id == other.stockAuditId
    }

    override fun hashCode(): Int {
        return stockAuditId?.hashCode() ?: System.identityHashCode(this)
    }
}
