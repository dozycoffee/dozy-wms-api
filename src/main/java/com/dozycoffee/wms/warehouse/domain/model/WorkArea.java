package com.dozycoffee.wms.warehouse.domain.model;

import com.dozycoffee.wms.global.common.BaseEntity;
import com.dozycoffee.wms.global.error.InvalidDomainValueException;
import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode;
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus;
import com.dozycoffee.wms.warehouse.domain.exception.InactiveWorkAreaException;
import com.dozycoffee.wms.warehouse.domain.exception.InsufficientWorkAreaCapacityException;
import com.dozycoffee.wms.warehouse.domain.exception.InvalidWorkAreaAmountException;
import com.dozycoffee.wms.warehouse.domain.exception.WorkAreaCapacityExceededException;
import com.dozycoffee.wms.warehouse.domain.exception.WorkAreaErrorCode;
import com.dozycoffee.wms.warehouse.domain.valueobject.Capacity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class WorkArea extends BaseEntity {

    private static final int INITIAL_USED_CAPACITY = 0;

    private final Long workAreaId;
    private final Long warehouseId;
    private final AreaCode areaCode;
    private int usedCapacity;
    private AvailabilityStatus workAreaStatus;

    public static WorkArea create(
            Long warehouseId,
            AreaCode areaCode,
            AvailabilityStatus workAreaStatus
    ) {
        validateWarehouseId(warehouseId);
        validateAreaCode(areaCode);
        validateWorkAreaStatus(workAreaStatus);
        return new WorkArea(null, warehouseId, areaCode, INITIAL_USED_CAPACITY, workAreaStatus);
    }

    public static WorkArea reconstitute(
            Long workAreaId,
            Long warehouseId,
            AreaCode areaCode,
            int usedCapacity,
            AvailabilityStatus workAreaStatus
    ) {
        return new WorkArea(workAreaId, warehouseId, areaCode, usedCapacity, workAreaStatus);
    }

    public String getAreaName() {
        return areaCode.getAreaName();
    }

    public Capacity getCapacity() {
        return areaCode.getCapacity();
    }

    /** 작업 구역 점유 */
    public void occupy(int amount) {
        validateAmount(amount);
        validateActive();
        validateCapacityNotExceeded(amount);
        this.usedCapacity += amount;
    }

    /** 작업 구역 반출 */
    public void release(int amount) {
        validateAmount(amount);
        validateActive();
        validateSufficientUsedCapacity(amount);
        this.usedCapacity -= amount;
    }

    private void validateAmount(int amount) {
        if (amount <= 0) {
            throw new InvalidWorkAreaAmountException();
        }
    }

    private void validateActive() {
        if (workAreaStatus != AvailabilityStatus.AVAILABLE) {
            throw new InactiveWorkAreaException();
        }
    }

    private void validateCapacityNotExceeded(int amount) {
        if (usedCapacity + amount > areaCode.getCapacity().value()) {
            throw new WorkAreaCapacityExceededException();
        }
    }

    private void validateSufficientUsedCapacity(int amount) {
        if (usedCapacity - amount < 0) {
            throw new InsufficientWorkAreaCapacityException();
        }
    }

    private static void validateWarehouseId(Long warehouseId) {
        if (warehouseId == null) {
            throw new InvalidDomainValueException(WorkAreaErrorCode.INVALID_WAREHOUSE_ID);
        }
    }

    private static void validateAreaCode(AreaCode areaCode) {
        if (areaCode == null) {
            throw new InvalidDomainValueException(WorkAreaErrorCode.INVALID_AREA_CODE);
        }
    }

    private static void validateWorkAreaStatus(AvailabilityStatus workAreaStatus) {
        if (workAreaStatus == null) {
            throw new InvalidDomainValueException(WorkAreaErrorCode.INVALID_WORK_AREA_STATUS);
        }
    }
}
