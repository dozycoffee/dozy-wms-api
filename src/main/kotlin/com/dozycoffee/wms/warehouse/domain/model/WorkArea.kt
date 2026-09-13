package com.dozycoffee.wms.warehouse.domain.model

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.global.error.DomainValidator.requireNonNull
import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus
import com.dozycoffee.wms.warehouse.domain.exception.InactiveWorkAreaException
import com.dozycoffee.wms.warehouse.domain.exception.InsufficientWorkAreaCapacityException
import com.dozycoffee.wms.warehouse.domain.exception.InvalidWorkAreaAmountException
import com.dozycoffee.wms.warehouse.domain.exception.WorkAreaCapacityExceededException
import com.dozycoffee.wms.warehouse.domain.exception.WorkAreaErrorCode
import com.dozycoffee.wms.warehouse.domain.valueobject.Capacity

class WorkArea private constructor(
    val workAreaId: Long?,
    val warehouseId: Long,
    val areaCode: AreaCode,
    usedCapacity: Int,
    val workAreaStatus: AvailabilityStatus
) : BaseEntity() {

    var usedCapacity: Int = usedCapacity
        private set

    val areaName: String get() = areaCode.areaName
    val capacity: Capacity get() = areaCode.capacity

    companion object {
        private const val INITIAL_USED_CAPACITY = 0

        fun create(
            warehouseId: Long?,
            areaCode: AreaCode?,
            workAreaStatus: AvailabilityStatus?
        ): WorkArea {
            val validWarehouseId: Long = requireNonNull(warehouseId, WorkAreaErrorCode.INVALID_WAREHOUSE_ID)
            val validAreaCode: AreaCode = requireNonNull(areaCode, WorkAreaErrorCode.INVALID_AREA_CODE)
            val validStatus: AvailabilityStatus =
                requireNonNull(workAreaStatus, WorkAreaErrorCode.INVALID_WORK_AREA_STATUS)
            return WorkArea(null, validWarehouseId, validAreaCode, INITIAL_USED_CAPACITY, validStatus)
        }

        fun reconstitute(
            workAreaId: Long,
            warehouseId: Long,
            areaCode: AreaCode,
            usedCapacity: Int,
            workAreaStatus: AvailabilityStatus
        ): WorkArea {
            return WorkArea(workAreaId, warehouseId, areaCode, usedCapacity, workAreaStatus)
        }
    }

    /** 작업 구역 점유 */
    fun occupy(amount: Int) {
        validateAmount(amount)
        validateActive()
        validateCapacityNotExceeded(amount)
        usedCapacity += amount
    }

    /** 작업 구역 반출 */
    fun release(amount: Int) {
        validateAmount(amount)
        validateActive()
        validateSufficientUsedCapacity(amount)
        usedCapacity -= amount
    }

    private fun validateAmount(amount: Int) {
        if (amount <= 0) {
            throw InvalidWorkAreaAmountException()
        }
    }

    private fun validateActive() {
        if (workAreaStatus != AvailabilityStatus.AVAILABLE) {
            throw InactiveWorkAreaException()
        }
    }

    private fun validateCapacityNotExceeded(amount: Int) {
        if (usedCapacity + amount > areaCode.capacity.value) {
            throw WorkAreaCapacityExceededException()
        }
    }

    private fun validateSufficientUsedCapacity(amount: Int) {
        if (usedCapacity - amount < 0) {
            throw InsufficientWorkAreaCapacityException()
        }
    }
}
