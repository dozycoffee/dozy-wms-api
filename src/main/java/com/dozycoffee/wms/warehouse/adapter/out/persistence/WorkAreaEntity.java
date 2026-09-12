package com.dozycoffee.wms.warehouse.adapter.out.persistence;

import com.dozycoffee.wms.global.common.BaseEntity;
import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode;
import com.dozycoffee.wms.warehouse.domain.enumeration.AvailabilityStatus;
import com.dozycoffee.wms.warehouse.domain.model.WorkArea;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("work_area")
@Getter
public class WorkAreaEntity extends BaseEntity {

    private static final String AREA_CODE_GROUP = "WORK_AREA_TYPE";
    private static final String STATUS_GROUP = "WORK_AREA_STATUS";

    @Id
    private Long workAreaId;
    private Long warehouseId;
    private String areaCode;
    private int usedCapacity;
    private String workAreaStatus;

    private WorkAreaEntity() {
    }

    public WorkArea toDomain() {
        return WorkArea.reconstitute(
                workAreaId,
                warehouseId,
                CommonCodes.fromCode(AreaCode.class, areaCode),
                usedCapacity,
                CommonCodes.fromCode(AvailabilityStatus.class, workAreaStatus)
        );
    }

    public static WorkAreaEntity from(WorkArea domain) {
        WorkAreaEntity entity = new WorkAreaEntity();
        entity.workAreaId = domain.getWorkAreaId();
        entity.warehouseId = domain.getWarehouseId();
        entity.areaCode = CommonCodes.toCode(AREA_CODE_GROUP, domain.getAreaCode());
        entity.usedCapacity = domain.getUsedCapacity();
        entity.workAreaStatus = CommonCodes.toCode(STATUS_GROUP, domain.getWorkAreaStatus());
        return entity;
    }
}
